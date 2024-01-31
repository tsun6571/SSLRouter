/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package jp.co.verisign.sslrouter;

import java.io.Reader;
import java.io.FileNotFoundException;
import java.io.IOException;
import com.ibatis.common.resources.Resources;
import com.ibatis.sqlmap.client.SqlMapClient;
import com.ibatis.sqlmap.client.SqlMapClientBuilder;
import java.io.FileInputStream;
import java.sql.SQLException;

/**
 *
 * @author t-yamamoto
 */
public class RouteInfoQuery {

    private SqlMapClient sqlMap = null;

    public RouteInfoQuery(String xmlFile) throws FileNotFoundException, IOException {
        if(sqlMap == null){
            // Initialize iBATIS using SqlMapConfig.xml
            Reader reader = Resources.getResourceAsReader(xmlFile);
            sqlMap = SqlMapClientBuilder.buildSqlMapClient(reader);
            //FileInputStream is = new FileInputStream(xmlFile);
            //sqlMap = SqlMapClientBuilder.buildSqlMapClient(is);
        }
    }

    public RouteInfo ByClientCertInfoAndRouteFromRouter(String s_cci, String s_rfr) throws SQLException{
        RouteInfo qp = new RouteInfo();
        qp.setClient_cert_info(s_cci);
        qp.setRoute_from_router(s_rfr);
        RouteInfo ri = (RouteInfo)sqlMap.queryForObject("RouteInfoByClientCertInfoAndRouteFromRouter", qp);

        return ri;
    }

}
