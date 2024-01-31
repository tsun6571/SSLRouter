/*
 * TransactionLog.java
 *
 * Created on 2010/04/22, 22:23
 *
 * To change this template, choose Tools | Template Manager
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
public class TransactionLog {

    private SqlMapClient sqlMap = null;

    public TransactionLog(String xmlFile) throws FileNotFoundException, IOException {
        if(sqlMap == null){
            // Initialize iBATIS using SqlMapConfig.xml
            Reader reader = Resources.getResourceAsReader(xmlFile);
            sqlMap = SqlMapClientBuilder.buildSqlMapClient(reader);
            //FileInputStream is = new FileInputStream(xmlFile);
            //sqlMap = SqlMapClientBuilder.buildSqlMapClient(is);
        }
    }

    public void insert(Transaction t) throws SQLException {
        
        sqlMap.insert("insertTransactionLog", t);
    }

}
