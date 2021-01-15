<%@ page import="java.util.Map" %>
<%@ page import="org.springframework.beans.factory.annotation.Autowired" %>
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
    <title>Resource Migration Status Page</title>
</head>
<body>

<div id="header" style="width:100%">

    <div id="statusMessage" style="float:left; width:80%">
        ${statusMessage}
    </div>

    <div id="menubar" style="float:left; width:20%">
        <a href="/status">Refresh</a> &nbsp;&nbsp;
        <a href="/config">Config</a> &nbsp;&nbsp;
        <a href="/javasimon">JavaSimon</a>
    </div>

</div>

<br/>
<br/>


<div id="reports" style="width:100%">

    <div id="resourceReport" style="float:left; width:50%">

        <table cellpadding="2">
            <caption>Resource Migration </caption>
            <c:forEach var="entry" items="${resourceReportMap}">
                <tr>
                    <td><c:out value="${entry.key}"/></td>
                    <td><c:out value="${entry.value}"/></td>
                </tr>
            </c:forEach></table>

    </div>

    <div id="slideReport" style="float:left; width:50%">

        <table cellpadding="2">
            <caption>Slide Migration</caption>
            <c:forEach var="entry" items="${slideThumbsReportMap}">
                <tr>
                    <td><c:out value="${entry.key}"/></td>
                    <td><c:out value="${entry.value}"/></td>
                </tr>
            </c:forEach></table>

    </div>

</div>

<br/>
<br/>


<div style="clear: left">

    <br/>
    <br/>

    <table cellpadding="2">
        <caption>Error Counts</caption>
        <c:forEach var="entry" items="${errorMap}">
            <tr>
                <td><c:out value="${entry.key}"/></td>
                <td><c:out value="${entry.value}"/></td>
            </tr>
        </c:forEach></table>

</div>

</body>
</html>
