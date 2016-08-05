package org.xdi.oxauth.rp.demo;

import org.apache.log4j.Logger;
import org.xdi.oxauth.client.UserInfoClient;
import org.xdi.oxauth.client.UserInfoResponse;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;

/**
 * @author yuriyz on 07/19/2016.
 */
public class RpDemoServlet extends HttpServlet {

    private static final Logger LOG = Logger.getLogger(RpDemoServlet.class);

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            resp.setContentType("text/html;charset=utf-8");

            PrintWriter pw = resp.getWriter();
            pw.println("<h1>RP Demo</h1>");
            pw.println("<br/><br/>");

            String accessToken = (String) req.getSession().getAttribute("access_token");
            String userInfoEndpoint = (String) req.getSession().getAttribute("userinfo_endpoint");

            LOG.trace("access_token: " + accessToken + ", userinfo_endpoint: " + userInfoEndpoint);

            UserInfoClient userInfoClient = new UserInfoClient(userInfoEndpoint);
            userInfoClient.setExecutor(Utils.createTrustAllExecutor());
            UserInfoResponse response = userInfoClient.execUserInfo(accessToken);
            LOG.trace("UserInfo response: " + response);

            if (response.getStatus() != 200) {
                pw.print("Failed to fetch user info claims");
                return;
            }

            pw.println("<h2>User Info Claims:</h2>");
            pw.println("<br/>");

            for (Map.Entry<String, List<String>> entry : response.getClaims().entrySet()) {
                pw.print("Name: " + entry.getKey() + " Value: " + entry.getValue());
                pw.println("<br/>");
            }
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }
}
