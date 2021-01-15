<%--
  Created by IntelliJ IDEA.
  User: greg
  Date: 10/7/16
  Time: 11:05 AM
  To change this template use File | Settings | File Templates.
--%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<html>
<head>
    <title>Partitioning Server - Config Page</title>
</head>
<body>

<div>
    <a href="/config">Refresh</a> &nbsp;&nbsp;
    <a href="/status">Status</a> &nbsp;&nbsp;
    <a href="/javasimon">JavaSimon</a>
</div>

<br/>

<div>
    ${statusMsg}
</div>

<br/>
<br/>

<div>
    Available Memory: <%= (Runtime.getRuntime().freeMemory() / 1000 / 1000) %>Mb of <%= (Runtime.getRuntime().totalMemory()/1000/1000) %> Mb
</div>

<br/>
<br/>

<div>

    <table cellpadding="2">
        <caption>Confg.properties</caption>
        <c:forEach var="entry" items="${configMap}">
        <tr>
            <td><c:out value="${entry.key}"/></td>
            <td><c:out value="${entry.value}"/></td>
        </tr>
    </c:forEach></table>

</div>


</body>
</html>
