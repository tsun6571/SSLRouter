/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package jp.co.verisign.sslrouter;

/**
 *
 * @author tsunoda
 */
public class RouteInfo {

    private int id;
    private String client_cert_info;
    private String route_from_router;
    private String route_to_host;
    private int route_to_port;
    private int log_level;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getRoute_from_router() {
        return route_from_router;
    }

    public void setRoute_from_router(String route_from_router) {
        this.route_from_router = route_from_router;
    }

    public String getClient_cert_info() {
        return client_cert_info;
    }

    public void setClient_cert_info(String client_cert_info) {
        this.client_cert_info = client_cert_info;
    }

    public int getLog_level() {
        return log_level;
    }

    public void setLog_level(int log_level) {
        this.log_level = log_level;
    }

    public String getRoute_to_host() {
        return route_to_host;
    }

    public void setRoute_to_host(String route_to_host) {
        this.route_to_host = route_to_host;
    }

    public int getRoute_to_port() {
        return route_to_port;
    }

    public void setRoute_to_port(int route_to_port) {
        this.route_to_port = route_to_port;
    }
}
