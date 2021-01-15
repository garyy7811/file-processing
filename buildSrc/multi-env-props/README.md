Flashflexpro Gradle Plugin ---- multi-env-props
=========================
Multi environment properties management. 

Minimize the possibility missing properties files and/or properties in a file by catching mistakes when comparing with a root environment 

Clone environment and filter replace strings in property values

Distribute property files into each sub projects.

##Concept
Building multi projects into multi environments is challenging, especially when we have millions of code, tens even hundreds of appcations to manage. 
This Gradle plugin is trying to help by managing properties files in each project, organize them in to different environments.

An environment here is a set of property files with the same file name in each sub project's dir, when user pick property files in Gradle commandline, 
user actually select a environment to build/deploy.

We assume that these environments' properties files have same properties names but different values 


####Setup
all path are relative to root project
##### gradle.properties
```properties
#optional, default value overrideProps, use in commandline: -PoverrideProps=<path to the overriding properties file> 
multiEnvProps_overrideEnvProps=overrideProps
#must, the name will be used in code to access the final configuration map in Gradle script usually your build.gradle
multiEnvProps_extMapVarName=ffpConfig
#must, this is the properties file in each project
multiEnvProps_rootPropsFileName=ffp.properties

#optional, the file that contains the property files' paths being used
multiEnvProps_currentPropsInfo=local_default_env.config
```
##### build.gradle
```groovy
buildscript {
    repositories {
        mavenCentral()
    }

    dependencies {
        classpath 'com.flashflexpro:multi-env-props:0.0.2'
    }
}

apply plugin: "com.flashflexpro.multenvprops"

multiEnvConfig{

    //keys are to be replaced when cloning new environment, values are default value
    propValReplacements  = [ "ffp":"defaultNewEnvName", "localhost":"defaultNewDeployHost" ]
    
}
```

#### Typically:
You have three testing VCS branches:

Trunk, the branch that everyone commit code to usually, is where people resolves conflicts, running unit tests, compile/test/deploy fast and fail fast so that developers know what the others are trying to do in code quickly, the trunk code usually deploy to local servers for internal use;

Test, merged from trunk, the branch that has a lot of running data so that people can really use it simulating realistic cases to verify the code actually work with realistic data in GUI tests.

Staging, merged from test, with configurations as closer to production environments as possible.

Production, merged from staging or just use staging branch but different configurations.


These branches have different purposes, should be build with different configurations and deployed into different environments.
For example, they have mostly same configuration names like server address, credentials, the content, but configuration values are different, test branch have 
```properties
deployTargetServerURL: http://my-test-env-server-internal-address
deployTargetServerUsr: http://my-test-env-server-admin-user
deployTargetServerPss: http://my-test-env-server-admin-pass
```

Trunk deploy configurations: 
 
```properties
deployTargetServerURL: http://my-trunk-env-server-internal-address
deployTargetServerUsr: http://my-trunk-env-server-admin-user
deployTargetServerPss: http://my-trunk-env-server-admin-pass
```
This multi-env-props are here to help manage these properties for building multi projects in multi branches/environments, typically it has one environment property files as root, this github project is an example using ffp.properties file, 
each application project has a ffp.properties configuring how its application build and deployed, with this multi-env-props Gradle plugin, 
it's very easy to share, override and verify properties, to create or remove new environments.

Mandatory configurations, gradle.properties 

