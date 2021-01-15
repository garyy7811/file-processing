<%@ page import="org.pubanatomy.migrateResources.status.SlideMigrationReport" %>
<%@ page import="java.util.Map" %>
<%@ page import="org.springframework.beans.factory.annotation.Autowired" %>
<%@ page import="org.pubanatomy.migrateResources.status.SlideStats" %><%--
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
    <title>Migrate Resources Status Page</title>
</head>
<body>

<div id="header" style="width:100%">

    <div id="statusMessage" style="float:left; width:40%">
        ${statusMessage}
    </div>

    <div id="statusSummary" style="float:left; width:40%">
        ${statusSummary}
    </div>

    <div id="menubar" style="float:left; width:20%">
        <a href="status">Refresh</a> &nbsp;&nbsp;
        <a href="config">Config</a>
    </div>

</div>

<br/>
<br/>


<div id="reports" style="width:100%">

    <div id="resourceReport" style="float:left; width:50%">

        <table cellpadding="2">
            <caption>Resource Migration</caption>
            <tr>
                <td>Standard Processor State</td>
                <td>${slideThumbnailInputAdapterStatus} (<a href="/control?adapterName=resources&newState=start">Start</a>, <a href="/control?adapterName=resources&newState=stop">Stop</a>)</td>
            </tr>
            <tr>
                <td>Updates Processor State</td>
                <td>n/a</td>
            </tr>
            <c:forEach var="entry" items="${resourceMigrationReportMap}">
            <tr>
                <td><c:out value="${entry.key}"/></td>
                <td><c:out value="${entry.value}"/></td>
            </tr>
        </c:forEach></table>

    </div>

    <div id="slideReport" style="float:left; width:50%">

        <table cellpadding="2">
            <caption>Slide Thumbs Migration</caption>

            <tr>
                <td>Standard Processor State</td>
                <td>${slideThumbnailInputAdapterStatus} (<a href="/control?adapterName=slides&newState=start">Start</a>, <a href="/control?adapterName=slides&newState=stop">Stop</a>)</td>
            </tr>
            <tr>
                <td>Updates Processor State</td>
                <td>${updatedSlideThumbnailInputAdapterStatus}</td>
            </tr>

            <c:forEach var="entry" items="${slideMigrationReportMap}">
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
