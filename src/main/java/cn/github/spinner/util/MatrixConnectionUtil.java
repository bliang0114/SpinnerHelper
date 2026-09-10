package cn.github.spinner.util;

import cn.github.driver.MQLException;
import cn.github.driver.connection.MatrixConnection;
import cn.github.driver.connection.MatrixSession;
import cn.github.spinner.config.EnvironmentConfig;
import cn.github.spinner.context.UserInput;
import cn.github.spinner.i18n.SpinnerBundle;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;

public final class MatrixConnectionUtil {
    private static final int REACHABILITY_TIMEOUT_MILLIS = 3000;

    private MatrixConnectionUtil() {
    }

    public static void assertCurrentServerReachable(@Nullable Project project) throws MQLException {
        if (project == null) {
            return;
        }
        EnvironmentConfig environment = UserInput.getInstance().connectEnvironment.get(project);
        if (environment == null) {
            return;
        }
        assertServerReachable(environment);
    }

    public static void assertServerReachable(@NotNull EnvironmentConfig environment) throws MQLException {
        ServerEndpoint endpoint = parseEndpoint(environment.getHostUrl());
        if (endpoint == null) {
            return;
        }
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(endpoint.host(), endpoint.port()), REACHABILITY_TIMEOUT_MILLIS);
        } catch (IOException e) {
            throw new MQLException("Server unreachable: " + endpoint.host() + ":" + endpoint.port(), e);
        }
    }

    public static @NotNull MatrixConnection openIndependentConnection(@NotNull Project project,
                                                                       @NotNull EnvironmentConfig environment)
            throws MQLException {
        EnvironmentConfig connected = UserInput.getInstance().connectEnvironment.get(project);
        if (connected == null || !Objects.equals(connected.getName(), environment.getName())
                || !Objects.equals(connected.getDriver(), environment.getDriver())
                || !Objects.equals(connected.getHostUrl(), environment.getHostUrl())
                || !Objects.equals(connected.getUser(), environment.getUser())
                || !Objects.equals(connected.getPassword(), environment.getPassword())
                || !Objects.equals(connected.getVault(), environment.getVault())
                || !Objects.equals(connected.getRole(), environment.getRole())
                || connected.isCas() != environment.isCas()) {
            throw new MQLException(SpinnerBundle.message("message.connected.environment.missing"));
        }
        return requireSession(UserInput.getInstance().connection.get(project)).openContext();
    }

    public static @NotNull MatrixSession requireSession(@Nullable MatrixConnection connection) throws MQLException {
        if (connection instanceof MatrixSession session) return session;
        throw new MQLException(SpinnerBundle.message("message.matrix.session.unsupported"));
    }

    public static void closeAsync(@Nullable Project project,
                                  @Nullable MatrixConnection connection,
                                  String actionName,
                                  @Nullable Runnable afterClose) {
        if (connection == null) {
            if (afterClose != null) {
                afterClose.run();
            }
            return;
        }
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                connection.close();
            } catch (IOException e) {
                UIUtil.showErrorNotification(project, actionName, SpinnerBundle.message("message.close.connection.failed", e.getLocalizedMessage()));
            } finally {
                if (afterClose != null) {
                    afterClose.run();
                }
            }
        });
    }

    private static @Nullable ServerEndpoint parseEndpoint(@Nullable String hostUrl) {
        if (hostUrl == null || hostUrl.isBlank()) {
            return null;
        }
        try {
            URI uri = new URI(hostUrl.trim());
            String host = uri.getHost();
            if (host == null || host.isBlank()) {
                return null;
            }
            int port = uri.getPort();
            if (port <= 0) {
                port = "https".equalsIgnoreCase(uri.getScheme()) ? 443 : 80;
            }
            return new ServerEndpoint(host, port);
        } catch (URISyntaxException ignored) {
            return null;
        }
    }

    private record ServerEndpoint(@NotNull String host, int port) {
    }
}
