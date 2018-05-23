package com.mycompany.tahiti.analysis.jena;

import com.mycompany.tahiti.analysis.utils.Utility;
import lombok.val;
import org.apache.jena.rdf.model.*;
import org.apache.jena.sdb.SDBFactory;
import org.apache.jena.sdb.Store;
import org.apache.jena.sdb.StoreDesc;
import org.apache.jena.sdb.sql.JDBC;
import org.apache.jena.sdb.sql.SDBConnection;
import org.apache.jena.sdb.store.DatabaseType;
import org.apache.jena.sdb.store.LayoutType;
import org.apache.jena.sdb.util.StoreUtils;
import org.apache.log4j.Logger;

import java.sql.SQLException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class MysqlJenaLibrary implements JenaLibrary{
    private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
    private static final Logger logger = Logger.getLogger(MysqlJenaLibrary.class.getName());
    private static final String NS = "http://knowledge.richInfo.com/";
    public Store store = null;

    /**
     * 以数据库连接初始化Store
     */
    public MysqlJenaLibrary(String jdbcUrl, String user, String pw) {
        StoreDesc desc = new StoreDesc(LayoutType.LayoutTripleNodesHash, DatabaseType.MySQL);
        // 加载mysql驱动
        JDBC.loadDriverMySQL();
        SDBConnection conn = new SDBConnection(jdbcUrl, user, pw);
        this.store = SDBFactory.connectStore(conn, desc);
        initDB();
        logger.info("initialize store using jdbcUrl");
    }

    /**
     * 以配置文件初始化Store
     */
    public MysqlJenaLibrary(String configFilePath) {
        this.store = SDBFactory.connectStore(Utility.getResourcePath(configFilePath));
        initDB();
        logger.info("initialize store using config file from " + configFilePath);
    }

    /**
     * 初始化mysql数据库
     */
    private void initDB() {
        try {
            if( StoreUtils.isFormatted(this.store) == false)
            {
                logger.info("initialize database");
                logger.info("create table format and truc data");
                store.getTableFormatter().create();
            }
        } catch (SQLException e) {
            logger.error(e);
            e.printStackTrace();
        }
    }

    /**
     * 清空数据
     */
    public void clearDB() {
        this.store.getTableFormatter().truncate();
        logger.info("clear all the data in the SDB");
    }

    public void closeDB() {
        this.store.close();
        logger.info("close store");
    }


    /**
     * 清除Store中的某个model中的数据;
     * @param modelName
     */
    public void removeModel(String modelName) {
        val writeLock = readWriteLock.writeLock();
        try {
            writeLock.lock();
            Model model = getModel(modelName);
            model.removeAll();
            logger.info("clear all the data in the model " + modelName);
        } finally {
            writeLock.unlock();
        }

    }
    /**
     * 获取store中的某个模型
     * @param modelName
     * @return
     */
    public Model getModel(String modelName) { return SDBFactory.connectNamedModel(this.store, modelName); }
    /**
     * 获取默认模型
     * @return
     */
    public Model getDefaultModel() { return SDBFactory.connectDefaultModel(this.store); }

    /**
     * 获取模型中所有Statement
     * @param model
     * @return
     */
    public List<Statement> getStatements(Model model) {
        val writeLock = readWriteLock.writeLock();
        List<Statement> stmts;
        try {
            writeLock.lock();
            StmtIterator sIter = model.listStatements() ;
            stmts = new LinkedList();
            for ( ; sIter.hasNext() ; )
            {
                stmts.add(sIter.nextStatement());
            }
            sIter.close();
        } finally {
            writeLock.unlock();
        }
        return stmts;
    }

    @Override
    public Iterator<Statement> getStatementsByEntityType(Model model, String type) {
        Property property = model.getProperty("type.object.type");
        SimpleSelector simpleSelector = new SimpleSelector(null, property, type);
        return model.listStatements(simpleSelector);
    }

    @Override
    public Iterator<Statement> getStatementsBySP(Model model, Resource resource, String property) {
        Property p = model.getProperty(property);
        SimpleSelector simpleSelector = new SimpleSelector(resource, p, (RDFNode) null);
        return model.listStatements(simpleSelector);
    }

    @Override
    public Iterator<Statement> getStatementsBySourceAndType(Model model, String source, String type) {
        SimpleSelector selector = new SimpleSelector(null, null, (RDFNode)null) {
            Property property = model.getProperty("type.object.type");
            public boolean selects(Statement st) {
                return st.getSubject().toString().startsWith(source) && Objects.equals(property, st.getPredicate()) && st.getObject().toString().equals(type);
            }
        };
        return model.listStatements(selector);
    }

    @Override
    public Iterator<Statement> getStatementsBySubjectSubStr(Model model, String substr) {
        SimpleSelector selector = new SimpleSelector(null, null, (RDFNode)null) {
            Property property = model.getProperty("type.object.type");
            public boolean selects(Statement st) {
                return st.getSubject().toString().contains(substr);
            }
        };
        return model.listStatements(selector);
    }
}
