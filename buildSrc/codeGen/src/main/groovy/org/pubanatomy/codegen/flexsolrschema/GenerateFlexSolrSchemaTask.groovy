package com.customshow.codegen.flexsolrschema

import org.apache.commons.lang.StringUtils
import org.apache.tools.ant.types.selectors.SelectorUtils
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.tasks.TaskAction

import java.beans.IntrospectionException
import java.lang.annotation.Annotation
import java.lang.reflect.Field
import java.lang.reflect.Modifier
import java.util.jar.JarEntry
import java.util.jar.JarInputStream

class GenerateFlexSolrSchemaTask extends DefaultTask {
    public static final String NAME = 'generateFlexSolrSchema'


    private Project[] javaSourceProjects;

    Project[] getJavaSourceProjects() {
        return javaSourceProjects
    }

    void setJavaSourceProjects(Project[] javaSourceProjects) {
        this.javaSourceProjects = javaSourceProjects
        javaSourceProjects.each { j ->
            this.dependsOn( j.tasks[ 'jar' ] )
        }
    }

    String[] includeClasses;
    String[] excludeClasses;

    File outputDirectory;

    String schemaPath = "com.company.search.schema";
    String fieldPath = "com.company.search.field";

    @TaskAction
    def exeGenerateFlexSolrSchema() {

        if (includeClasses == null || includeClasses.length == 0) {
            throw new IllegalArgumentException("no solr bean classes included, check configure!");
        }


        if (!outputDirectory.exists()) {
            outputDirectory.mkdirs();
        }

        Map<String, File> classToJarFile = new HashMap<>();

        List<String> jarPaths = new ArrayList<>();
        javaSourceProjects.each { p ->
            File f = p.file(p.jar.archivePath)
            jarPaths.add(f.getAbsolutePath())
            JarInputStream jar = new JarInputStream(new FileInputStream(f));

            JarEntry jarEntry;
            while (true) {
                jarEntry = jar.getNextJarEntry();

                if (jarEntry == null) {
                    break;
                }

                String className = jarEntry.getName();

                if (jarEntry.isDirectory() || !className.endsWith(".class")) {
                    continue;
                }

                className = className.replace('/', '.');
                className = className.substring(0, className.length() - 6);

                if (matchWildCard(className, includeClasses) &&
                        !matchWildCard(className, excludeClasses)) {
                    classToJarFile.put(className, f);
                }
            }
        }


        if (classToJarFile.size() == 0) {
            throw new RuntimeException("No Classes found to generate: jar paths:" + StringUtils.join(jarPaths, ";") +
                    "; includeDtoClasses:" + includeClasses + "; excludeDtoClasses:" +
                    excludeClasses);
        }

        logger.info(classToJarFile.size() + " classes found to generate ")

        URLClassLoader clsLdr = null;

        try {
            List<URL> classpathsUrls = new ArrayList<URL>();

            // add all the jars to the new child realm
            for (String path : jarPaths) {
                URL url = new File(path).toURI().toURL();
                classpathsUrls.add(url);
            }


            def array = classpathsUrls.toArray(new URL[classpathsUrls.size()])

            def loader = Thread.currentThread().getContextClassLoader()
            clsLdr = new URLClassLoader(array, loader);

            for (String clsKey : classToJarFile.keySet()) {
                Class<? extends Serializable> tmpCls = (Class<? extends Serializable>) clsLdr.loadClass(clsKey);

                SchemaMxmlClassGenerating classGenerating = new SchemaMxmlClassGenerating(tmpCls, schemaPath, fieldPath);
                String code = classGenerating.generateCode();
                String clzName = classGenerating.getClassName();

                writeFile(clzName, code);
            }

        }
        catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        catch (IntrospectionException e) {
            e.printStackTrace();
        }
    }

    def void writeFile(String clzName, String code) {
        File dir = new File(outputDirectory.getAbsolutePath());
        dir.mkdirs();
        FileWriter fr = null;
        try {
            fr = new FileWriter(new File(dir.getAbsolutePath() + File.separator + clzName + ".mxml"));
            fr.write(code);
            fr.flush();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        finally {
            if (fr != null) {
                try {
                    fr.close();
                }
                catch (IOException e) {
                }
            }
        }
    }

    def boolean matchWildCard(String className, String[] wildCards) {
        if (wildCards == null) {
            return false;
        }

        for (String wildCard : wildCards) {
            if (className.equals(wildCard)) {
                return true;
            }

            if (SelectorUtils.matchPath(wildCard, className)) {
                return true;
            }
        }

        return false;
    }

    private static class SchemaMxmlClassGenerating {

        private final Class<? extends Serializable> javaClass;
        private String schemaPath;
        private String fieldPath;

        public SchemaMxmlClassGenerating(Class<? extends Serializable> tmpCls, String schemaPath, String fieldPath) {
            javaClass = tmpCls;
            this.schemaPath = schemaPath;
            this.fieldPath = fieldPath;
        }


        private Map<String, Field> solrFieldsMapLeftInClazz;

        public String generateCode() throws IntrospectionException {

            solrFieldsMapLeftInClazz = getSolrFieldAnnotationsMap(new HashMap<String, Field>(), javaClass);

            if (!Modifier.isAbstract(javaClass.getModifiers()) && solrFieldsMapLeftInClazz.size() == 1) {
                throw new RuntimeException(javaClass.getCanonicalName() + " has no properties!!!");
            }

            List<String> lst = new ArrayList<>();

            for (Iterator<Field> iterator = solrFieldsMapLeftInClazz.values().iterator(); iterator.hasNext();) {
                Field field = iterator.next();
                if (!field.getName().startsWith("query_") && !field.getName().equals("uid") &&
                        !field.getName().equals("enstcText")) {
                    lst.add(getCodeForEachField(field));
                }
            }


            String content = StringUtils.join(lst, "\n");
            String superClzName = getSuperClassName();
            if (superClzName.equals("SolrBeanSchema")) {
                superClzName = "solr:" + superClzName;
            } else {
                superClzName = "schema:" + superClzName;
            }

            String className = getClassName();
            return "<?xml version=\"1.0\"?>\n" +
                    "<" + superClzName + " xmlns:fx=\"http://ns.adobe.com/mxml/2009\"\n" +
                    "                       xmlns:ap=\"org.apache.flex.collections.*\"\n" +
                    "                       xmlns:solr=\"com.customshow.solr.*\"\n" +
                    "                                xmlns:schema=\"" + schemaPath + ".*\"\n" +
                    "                                xmlns:field=\"" + fieldPath + ".*\">\n" +
                    "    <fx:Script><![CDATA[\n" +
                    "        private var _allAdded:Boolean = false;\n" +
                    "        override public function getAllFields():ArrayList{\n" +
                    "            if( !_allAdded ){\n" +
                    "                ___allFields.addAll( ___" + className + " );\n" +
                    "                _allAdded = true;\n" +
                    "            }\n" +
                    "            return super.getAllFields();\n" +
                    "        }\n" +
                    "        ]]></fx:Script>\n" +
                    "    <fx:Declarations>\n" +
                    "        <ap:ArrayList id=\"___" + className + "\">\n" +
                    content + "\n" +
                    "       </ap:ArrayList>\n" +
                    "    </fx:Declarations>\n" +
                    "</" + superClzName + ">\n";
        }

        private String getCodeForEachField(Field field) {
            org.apache.solr.client.solrj.beans.Field solrField =
                    field.getDeclaredAnnotation(org.apache.solr.client.solrj.beans.Field.class);
            String solrAnnot = solrField.value();

            if (!solrAnnot.contains("__")) {
                throw new IllegalArgumentException("ONly support __ now!");
            }

            String qStr = "";
            String qName = "query_" + field.getName();
            Field queryField = solrFieldsMapLeftInClazz.get(qName);
            if (queryField != null) {
                org.apache.solr.client.solrj.beans.Field querySolrField =
                        queryField.getDeclaredAnnotation(org.apache.solr.client.solrj.beans.Field.class);
                qStr = "queryField=\"" + querySolrField.value() + "\" ";
            }
            String tOrR = "R";

            Class<? extends Object> fieldOrCompType =
                    field.getType().isArray() ? field.getType().getComponentType() : field.getType();
            if (String.class.equals(fieldOrCompType)) {
                tOrR = "T";
            }


            return "            " +
                    "<field:QueryField" + tOrR +
                    " isArray=\"" + field.getType().isArray() + "\" " +
                    " isStrID=\"" + (solrAnnot.indexOf("__si_") > 0) + "\" " +
                    " id=\"" + solrAnnot + "\" " +
                    " name=\"" + field.getName() + "\" " +
                    " label=\"" + field.getName() + "\" " +
                    " searchModel=\"{searchModel}\" " +
                    qStr + "/>";
        }

        public String getClassName() {
            String[] split = javaClass.getCanonicalName().split("\\.");
            String s = split[split.length - 1];
            return s + "Schema";
        }

        public String getSuperClassName() {
            String[] split = javaClass.getSuperclass().getCanonicalName().split("\\.");
            String s = split[split.length - 1];
            return s + "Schema";
        }


        public static Map<String, Field> getSolrFieldAnnotationsMap(Map<String, Field> rt, Class clazz) {
            Field[] clazzFieldArr = clazz.getDeclaredFields();

            for (Field clazzField : clazzFieldArr) {
                Annotation[] clazzFieldAnnArr = clazzField.getDeclaredAnnotations();
                for (Annotation annotation : clazzFieldAnnArr) {
                    if (annotation.annotationType().equals(org.apache.solr.client.solrj.beans.Field.class)) {
                        rt.put(clazzField.getName(), clazzField);
                    }
                }
            }
            return rt;
        }
    }

}
