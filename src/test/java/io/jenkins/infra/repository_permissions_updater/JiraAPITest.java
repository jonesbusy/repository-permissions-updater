package io.jenkins.infra.repository_permissions_updater;

import io.jenkins.infra.repository_permissions_updater.helper.URLHelper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.function.Supplier;

import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
class JiraAPITest {

    private static final Supplier<String> JIRA_USER_QUERY = JiraAPITest::createUserQuery;
    private static final Supplier<String> JIRA_COMPONENTS_URL = JiraAPITest::createComponentsURL;

    private Properties backup;

    private static String createUserQuery() {
        return System.getProperty("jiraUrl", "https://issues.jenkins.io") +"/rest/api/2/user?username=%s";
    }

    private static String createComponentsURL() {
        return System.getProperty("jiraUrl", "https://issues.jenkins.io") + "/rest/api/2/project/JENKINS/components";
    }

    @BeforeEach
    public void reset() {
        backup = new Properties();
        backup.putAll(System.getProperties());
        JiraAPI.INSTANCE = null;
        URLHelper.instance().getURLStreamHandler().resetConnections();
    }

    @AfterEach
    void restore() {
        System.setProperties(backup);
    }

    @Test
    void testEnsureLoadedWrongBaseUrl() {
        System.setProperty("jiraUrl", "xx://issues.jenkins.io");
        Exception exception = assertThrows(IOException.class, () -> {
            JiraAPI.getInstance().getComponentId("FakeData");
        });
        String expectedMessage = "Failed to construct Jira URL";
        String actualMessage = exception.getMessage();
        Assertions.assertTrue(actualMessage.contains(expectedMessage));
    }

    @Test
    void testEnsureLoadedFailedToOpenConnection() throws IOException {
        URL fakeUrl = spy(URI.create(JIRA_COMPONENTS_URL.get()).toURL());
        URLHelper.instance().getURLStreamHandler().addConnection(fakeUrl, new IOException());
        Exception exception = assertThrows(IOException.class, () -> {
                    JiraAPI.getInstance().getComponentId("FakeData");
        });
        String expectedMessage = "Failed to open connection for Jira URL";
        String actualMessage = exception.getMessage();
        Assertions.assertTrue(actualMessage.contains(expectedMessage));
    }

    @Test
    void testEnsureLoadedFailedToGetConnection() throws IOException {
        URL fakeUrl = spy(URI.create(JIRA_COMPONENTS_URL.get()).toURL());
        var fakeHttpConnection = mock(HttpURLConnection.class);
        doThrow(ProtocolException.class).when(fakeHttpConnection).setRequestMethod(anyString());
        URLHelper.instance().getURLStreamHandler().addConnection(fakeUrl, fakeHttpConnection);
        Exception exception = assertThrows(IOException.class, () -> {
                    JiraAPI.getInstance().getComponentId("FakeData");
        });
        String expectedMessage = "Failed to set request method";
        String actualMessage = exception.getMessage();
        Assertions.assertTrue(actualMessage.contains(expectedMessage));
    }

    @Test
    void testEnsureLoadedFailedToConnect() throws IOException {
        URL fakeUrl = spy(URI.create(JIRA_COMPONENTS_URL.get()).toURL());
        var fakeHttpConnection = mock(HttpURLConnection.class);
        doThrow(IOException.class).when(fakeHttpConnection).connect();
        URLHelper.instance().getURLStreamHandler().addConnection(fakeUrl, fakeHttpConnection);
        Exception exception = assertThrows(IOException.class, () -> {
            JiraAPI.getInstance().getComponentId("FakeData");
        });
        String expectedMessage = "Failed to connect to Jira URL";
        String actualMessage = exception.getMessage();
        Assertions.assertTrue(actualMessage.contains(expectedMessage));
    }

    @Test
    void testEnsureLoadedFailedToParse() throws IOException {
        URL fakeUrl = spy(URI.create(JIRA_COMPONENTS_URL.get()).toURL());
        var fakeHttpConnection = mock(HttpURLConnection.class);
        when(fakeHttpConnection.getInputStream()).thenThrow(IOException.class);
        URLHelper.instance().getURLStreamHandler().addConnection(fakeUrl, fakeHttpConnection);
        Exception exception = assertThrows(IOException.class, () -> {
            JiraAPI.getInstance().getComponentId("FakeData");
        });
        String expectedMessage = "Failed to parse Jira response";
        String actualMessage = exception.getMessage();
        Assertions.assertTrue(actualMessage.contains(expectedMessage));
    }

    @Test
    void testEnsureLoadedSuccessToParse() throws IOException {
        URL fakeUrl = spy(URI.create(JIRA_COMPONENTS_URL.get()).toURL());
        var fakeHttpConnection = mock(HttpURLConnection.class);
        when(fakeHttpConnection.getInputStream()).thenReturn(Files.newInputStream(Path.of("src","test","resources", "components_12_07_2024.json")));
        URLHelper.instance().getURLStreamHandler().addConnection(fakeUrl, fakeHttpConnection);
        var id = JiraAPI.getInstance().getComponentId("42crunch-security-audit-plugin");
        Assertions.assertEquals("27235", id);
    }

    @Test
    void testIsUserPresentInternalMalformedUrlException() {
        System.setProperty("jiraUrl", "xx://issues.jenkins.io");
        Exception exception = assertThrows(RuntimeException.class, () -> {
            Assertions.assertFalse(JiraAPI.getInstance().isUserPresent("FakeUser"));
        });
        String expectedMessage = "Failed to construct Jira URL";
        String actualMessage = exception.getMessage();
        Assertions.assertTrue(actualMessage.contains(expectedMessage));
    }


    @Test
    void testIsUserPresentInternalFailedToOpenConnection() throws IOException {
        URL fakeUrl = spy(URI.create(String.format(JIRA_USER_QUERY.get(), "FakeUser")).toURL());
        URLHelper.instance().getURLStreamHandler().addConnection(fakeUrl, new IOException());
        Exception exception = assertThrows(RuntimeException.class, () -> {
            Assertions.assertFalse(JiraAPI.getInstance().isUserPresent("FakeUser"));
        });
        String expectedMessage = "Failed to open connection for Jira URL";
        String actualMessage = exception.getMessage();
        Assertions.assertTrue(actualMessage.contains(expectedMessage));
    }

    @Test
    void testIsUserPresentInternalFailedToGetConnection() throws IOException {
        URL fakeUrl = spy(URI.create(String.format(JIRA_USER_QUERY.get(), "FakeUser")).toURL());
        var fakeHttpConnection = mock(HttpURLConnection.class);
        doThrow(ProtocolException.class).when(fakeHttpConnection).setRequestMethod(anyString());
        URLHelper.instance().getURLStreamHandler().addConnection(fakeUrl, fakeHttpConnection);
        Exception exception = assertThrows(RuntimeException.class, () -> {
            Assertions.assertFalse(JiraAPI.getInstance().isUserPresent("FakeUser"));
        });
        String expectedMessage = "Failed to set request method";
        String actualMessage = exception.getMessage();
        Assertions.assertTrue(actualMessage.contains(expectedMessage));
    }

    @Test
    void testIsUserPresentInternalFailedToConnect() throws IOException {
        URL fakeUrl = spy(URI.create(String.format(JIRA_USER_QUERY.get(), "FakeUser")).toURL());
        var fakeHttpConnection = mock(HttpURLConnection.class);
        doThrow(IOException.class).when(fakeHttpConnection).connect();
        URLHelper.instance().getURLStreamHandler().addConnection(fakeUrl, fakeHttpConnection);
        Exception exception = assertThrows(RuntimeException.class, () -> {
            Assertions.assertFalse(JiraAPI.getInstance().isUserPresent("FakeUser"));
        });
        String expectedMessage = "Failed to connect to Jira URL";
        String actualMessage = exception.getMessage();
        Assertions.assertTrue(actualMessage.contains(expectedMessage));
    }

    @Test
    void testIsUserPresentInternalNotExistsException() throws IOException {
        URL fakeUrl = spy(URI.create(String.format(JIRA_USER_QUERY.get(), "FakeUser")).toURL());
        var fakeHttpConnection = mock(HttpURLConnection.class);
        when(fakeHttpConnection.getResponseCode()).thenThrow(IOException.class);
        URLHelper.instance().getURLStreamHandler().addConnection(fakeUrl, fakeHttpConnection);
        Assertions.assertFalse(JiraAPI.getInstance().isUserPresent("FakeUser"));
    }

    @Test
    void testIsUserPresentInternalNotExists() throws IOException {
        URL fakeUrl = spy(URI.create(String.format(JIRA_USER_QUERY.get(), "FakeUser")).toURL());
        var fakeHttpConnection = mock(HttpURLConnection.class);
        when(fakeHttpConnection.getResponseCode()).thenReturn(404);
        URLHelper.instance().getURLStreamHandler().addConnection(fakeUrl, fakeHttpConnection);
        Assertions.assertFalse(JiraAPI.getInstance().isUserPresent("FakeUser"));
    }

    @Test
    void testIsUserPresentInternalExists() throws IOException {
        URL fakeUrl = spy(URI.create(String.format(JIRA_USER_QUERY.get(), "FakeUser")).toURL());
        var fakeHttpConnection = mock(HttpURLConnection.class);
        when(fakeHttpConnection.getResponseCode()).thenReturn(200);
        URLHelper.instance().getURLStreamHandler().addConnection(fakeUrl, fakeHttpConnection);
        var result = JiraAPI.getInstance().isUserPresent("FakeUser");
        Assertions.assertTrue(result);
    }

}
