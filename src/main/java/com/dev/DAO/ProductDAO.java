package com.dev.DAO;

import com.dev.VO.Behavior;
import com.dev.VO.Parser;
import com.dev.VO.Pool;
import java.sql.ResultSet;

import java.sql.Connection;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Future;

public class ProductDAO extends abstDAO {
    private final static Pool POOL = Pool.getInstance();
    private final static ProductDAO PRODUCT_DAO = new ProductDAO();
    private final static String LINE = "+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+";

    private ProductDAO() { }

    public static ProductDAO getInstance() { return PRODUCT_DAO; }

    public int search(String productName, String searchOption) {

        // +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
        // log 테이블의 productname과 searchoption은 primary key
        //
        // productname과 searchoption이 동시에 일치하는 튜플이 존재시 1 반환
        // prodcutname과 searchoption 둘 중 하나라도 존재하지 않으면 0 반환
        // +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+

        int flag = 0;

        try {
            PreparedStatement preparedStatement;
            Connection connection = connect();

            preparedStatement = connection.prepareStatement("SELECT * FROM log WHERE productname = (?) AND searchoption = (?)");

            preparedStatement.setString(1, productName);
            preparedStatement.setString(2, searchOption);

            if (preparedStatement.executeQuery().next()) flag = 1;

            close(preparedStatement, connection);
        } catch (SQLException throwables) {
            System.out.println("[com.dev.DAO.ProductDAO::search]: " + throwables.getMessage());
            throwables.printStackTrace();
        }

        return flag;
    }

    public void insert(List<Behavior> behaviors) {

        // +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
        // logDatainsertPipe, urlDataInsertPipe, coupangDataInsertPipe, gmarketDataInsertPipe, streetDataInsertPipe
        // 5개의 쓰레드를 Threadpool에 데이터 로드 작업 생성 및 처리 요청
        //
        // --- Multithread ---
        // insertLogDataRunnable: log 테이블에 상품 이름, 검색 옵션, 시간 저장
        // insertUrlDataRunnable: urls 테이블에 상품 이름과 검색 옵션에 따른 url 저장
        // insertCoupangDataRunnable, insertGmarketDataRunnable, insertstreetDataRunnable: 쇼핑몰 테이블에 정보 저장
        // +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+

        final HashMap<Integer, HashMap<String, String>> coupang = behaviors.get(0).getList();
        final HashMap<Integer, HashMap<String, String>> gmarket = behaviors.get(1).getList();
        final HashMap<Integer, HashMap<String, String>> street = behaviors.get(2).getList();

        final Runnable insertLogDataRunnable = () -> {
            Thread.currentThread().setName("executable logDatainsertPipe");
            Pool.threadPrint("insertLogDataRunnable");

            try {
                PreparedStatement logPreparedStatement;
                Connection logConnection = connect();

                logPreparedStatement = logConnection.prepareStatement("INSERT INTO log VALUES(?, ?, sysdate())");

                logPreparedStatement.setString(1, POOL.getsearchSet()[0]);
                logPreparedStatement.setString(2, POOL.getsearchSet()[1]);

                logPreparedStatement.executeUpdate();

                close(logPreparedStatement, logConnection);
            } catch (SQLException throwables) {
                System.out.println("[com.dev.DAO.ProductDAO::insert -> Runnable insertLogData]: " + throwables.getMessage());
                throwables.printStackTrace();
            }
        };

        final Runnable insertUrlDataRunnable = () -> {
            HashMap<String, String> urls = Parser.getUrlsHashMap();

            Thread.currentThread().setName("executable urlDataInsertPipe");
            Pool.threadPrint("insertUrlDataRunnable");

            try {
                PreparedStatement urlPreparedStatement;
                Connection urlConnection = connect();

                urlPreparedStatement = urlConnection.prepareStatement("INSERT INTO urls VALUES(?, ?, ?, ?)");

                urlPreparedStatement.setString(1, POOL.getsearchSet()[0]);
                urlPreparedStatement.setString(2, POOL.getsearchSet()[1]);

                for (String siteName : urls.keySet()) {
                    urlPreparedStatement.setString(3, siteName);
                    urlPreparedStatement.setString(4, urls.get(siteName));

                    urlPreparedStatement.executeUpdate();
                }

                close(urlPreparedStatement, urlConnection);
            } catch (SQLException throwables) {
                System.out.println("[com.dev.DAO.ProductDAO::insert -> Runnable insertUrlData]: " + throwables.getMessage());
                throwables.printStackTrace();
            }
        };

        final Runnable insertCoupangDataRunnable = () -> {
            Thread.currentThread().setName("executable coupangDataInsertPipe");
            Pool.threadPrint("insertCoupangDataRunnable");

            try {
                PreparedStatement coupangPreparedStatement;
                Connection coupangConneciton = connect();

                coupangPreparedStatement = coupangConneciton.prepareStatement("INSERT INTO coupang VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?)");

                insertSettor(coupangPreparedStatement, coupang);

                close(coupangPreparedStatement, coupangConneciton);
            } catch (SQLException throwables) {
                System.out.println("[com.dev.DAO.ProductDAO::insert -> Runnable insertCoupangData]: " + throwables.getMessage());
                throwables.printStackTrace();
            }
        };

        final Runnable insertGmarketDataRunnable = () -> {
            Thread.currentThread().setName("executable gmarketDataInsertPipe");
            Pool.threadPrint("insertGmarketDataRunnable");

            try {
                PreparedStatement gmarketPreparedStatement;
                Connection gmarketConneciton = connect();

                gmarketPreparedStatement = gmarketConneciton.prepareStatement("INSERT INTO gmarket VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?)");

                insertSettor(gmarketPreparedStatement, gmarket);

                close(gmarketPreparedStatement, gmarketConneciton);
            } catch (SQLException throwables) {
                System.out.println("[com.dev.DAO.ProductDAO::insert -> Runnable insertGmarketData]: " + throwables.getMessage());
                throwables.printStackTrace();
            }
        };

        final Runnable insertstreetDataRunnable = () -> {
            try {
                Thread.currentThread().setName("executable streetDataInsertPipe");

                Pool.threadPrint("insertstreetDataRunnable");

                PreparedStatement streetPreparedStatement;
                Connection streetConneciton = connect();

                streetPreparedStatement = streetConneciton.prepareStatement("INSERT INTO street VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?)");

                insertSettor(streetPreparedStatement, street);

                close(streetPreparedStatement, streetConneciton);
            } catch (SQLException throwables) {
                System.out.println("[com.dev.DAO.ProductDAO::insert -> Runnable insertstreetData]: " + throwables.getMessage());
                throwables.printStackTrace();
            }
        };

        System.out.println("Data insert multithread executed\n" + LINE);

        Future<?> logRunnableFuture = POOL.getExecutorService().submit(insertLogDataRunnable);
        Future<?> urlRunnableFuture = POOL.getExecutorService().submit(insertUrlDataRunnable);
        Future<?> coupangRunnableFuture = POOL.getExecutorService().submit(insertCoupangDataRunnable);
        Future<?> gmarketRunnableFuture = POOL.getExecutorService().submit(insertGmarketDataRunnable);
        Future<?> streetRunnableFuture = POOL.getExecutorService().submit(insertstreetDataRunnable);

        while (true) {
            if (logRunnableFuture.isDone() && urlRunnableFuture.isDone()
                    && coupangRunnableFuture.isDone() && gmarketRunnableFuture.isDone() && streetRunnableFuture.isDone()) {
                System.out.println("Data insert complete - product name: " + Pool.getInstance().getsearchSet()[0] + "\n" + LINE);

                break;
            }
        }
    }

    private static void insertSettor(PreparedStatement preparedStatement,
                                     HashMap<Integer, HashMap<String, String>> list) throws SQLException {
        preparedStatement.setString(6, POOL.getsearchSet()[0]);
        preparedStatement.setString(7, POOL.getsearchSet()[1]);

        for (int i = 1; i < list.size() + 1; i++) {
            HashMap<String, String> tree = list.get(i);

            preparedStatement.setString(1, tree.get("productName"));
            preparedStatement.setString(2, tree.get("productPrice"));
            preparedStatement.setString(3, tree.get("productHref"));
            preparedStatement.setString(4, tree.get("productImageSrc"));
            preparedStatement.setInt(5, i);
            preparedStatement.setString(8, tree.get("productRatingStar"));
            preparedStatement.setString(9, tree.get("productRatingCount"));

            preparedStatement.executeUpdate();
        }
    }

    public List<Behavior> load(String productName, String searchOption) {

        // +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
        // urlDataLoadingPipe, coupangDataLoadingPipe, gmarketDataLoadingPipe, streetDataLoadingPipe
        // 4개의 쓰레드를 Threadpool에 데이터 로드 작업 생성 및 처리 요청
        //
        // --- Multithread ---
        // loadUrlDataRunnable: urls 테이블에서 상품 이름과 검색 옵션에 따른 url 로딩 후 com.dev.VO.Parser:setUrl 실행
        // loadCoupangDataRunnable, loadGmarketDataRunnable, loadStreetDataRunnable: 쇼핑몰 테이블에서 정보 로딩
        // 4개의 쓰레드 작업이 모두 완료 시에는 각각의 Behavior의 setList 실행 후 List<Behavior> 반환
        // +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+

        final HashMap<Integer, HashMap<String, String>> coupangTree = new HashMap<>();
        final HashMap<Integer, HashMap<String, String>> gmarketTree = new HashMap<>();
        final HashMap<Integer, HashMap<String, String>> streetTree = new HashMap<>();

        final Runnable loadUrlDataRunnable = () -> {
            Thread.currentThread().setName("executable urlDataLoadingPipe");
            Pool.threadPrint("loadUrlDataRunnable");

            try {
                PreparedStatement urlPreparedStatement;
                Connection urlConneciton = connect();

                urlPreparedStatement = urlConneciton.prepareStatement("SELECT * FROM urls WHERE productname = (?) AND searchoption = (?)");

                urlPreparedStatement.setString(1, productName);
                urlPreparedStatement.setString(2, searchOption);

                ResultSet urlResultSet = urlPreparedStatement.executeQuery();

                while (urlResultSet.next())
                    Parser.setUrl(urlResultSet.getString("searchsite"), urlResultSet.getString("url"));

                close(urlPreparedStatement, urlConneciton, urlResultSet);
            } catch (SQLException throwables) {
                System.out.println("[com.dev.DAO.ProductDAO::load -> Runnable loadUrlDataRunnable]: " + throwables.getMessage());
                throwables.printStackTrace();
            }
        };

        final Runnable loadCoupangDataRunnable = () -> {
            Thread.currentThread().setName("executable coupangDataLoadingPipe");
            Pool.threadPrint("loadCoupangDataRunnable");

            try {
                PreparedStatement coupangPreparedStatement;
                Connection coupangConneciton = connect();

                coupangPreparedStatement = coupangConneciton.prepareStatement("SELECT * FROM coupang WHERE productname = (?) AND searchoption = (?) ORDER BY ranking");

                coupangPreparedStatement.setString(1, productName);
                coupangPreparedStatement.setString(2, searchOption);

                ResultSet coupangResultSet = coupangPreparedStatement.executeQuery();

                while (coupangResultSet.next())
                    loadSettor(coupangResultSet, new HashMap<>(), coupangTree);

                close(coupangPreparedStatement, coupangConneciton, coupangResultSet);
            } catch (SQLException throwables) {
                System.out.println("[com.dev.DAO.ProductDAO::load -> Runnable loadCoupangDataRunnable]: " + throwables.getMessage());
                throwables.printStackTrace();
            }
        };

        final Runnable loadGmarketDataRunnable = () -> {
            Thread.currentThread().setName("executable gmarketDataLoadingPipe");
            Pool.threadPrint("loadGmarketDataRunnable");

            try {
                PreparedStatement gmarketPreparedStatement;
                Connection gmarketConneciton = connect();

                gmarketPreparedStatement = gmarketConneciton.prepareStatement("SELECT * FROM gmarket WHERE productname = (?) AND searchoption = (?) ORDER BY ranking");

                gmarketPreparedStatement.setString(1, productName);
                gmarketPreparedStatement.setString(2, searchOption);

                ResultSet gmarketResultSet = gmarketPreparedStatement.executeQuery();

                while (gmarketResultSet.next())
                    loadSettor(gmarketResultSet, new HashMap<>(), gmarketTree);

                close(gmarketPreparedStatement, gmarketConneciton, gmarketResultSet);
            } catch (SQLException throwables) {
                System.out.println("[com.dev.DAO.ProductDAO::load -> Runnable loadGmarketDataRunnable]: " + throwables.getMessage());
                throwables.printStackTrace();
            }
        };

        final Runnable loadStreetDataRunnable = () -> {
            Thread.currentThread().setName("executable streetDataLoadingPipe");
            Pool.threadPrint("loadStreetDataRunnable");

            try {
                PreparedStatement gmarketPreparedStatement;
                Connection gmarketConneciton = connect();

                gmarketPreparedStatement = gmarketConneciton.prepareStatement("SELECT * FROM street WHERE productname = (?) AND searchoption = (?) ORDER BY ranking");

                gmarketPreparedStatement.setString(1, productName);
                gmarketPreparedStatement.setString(2, searchOption);

                ResultSet streetResultSet = gmarketPreparedStatement.executeQuery();

                while (streetResultSet.next())
                    loadSettor(streetResultSet, new HashMap<>(), streetTree);

                close(gmarketPreparedStatement, gmarketConneciton, streetResultSet);
            } catch (SQLException throwables) {
                System.out.println("[com.dev.DAO.ProductDAO::load -> Runnable loadStreetDataRunnable]: " + throwables.getMessage());
                throwables.printStackTrace();
            }
        };

        System.out.println(LINE + "\nData loading multithread executed\n" + LINE);

        final Future<?> urlRunnableFuture = POOL.getExecutorService().submit(loadUrlDataRunnable);
        final Future<?> coupangRunnableFuture = POOL.getExecutorService().submit(loadCoupangDataRunnable);
        final Future<?> gmarketRunnableFuture = POOL.getExecutorService().submit(loadGmarketDataRunnable);
        final Future<?> streetRunnableFuture = POOL.getExecutorService().submit(loadStreetDataRunnable);

        while (true) {
            if (urlRunnableFuture.isDone() && coupangRunnableFuture.isDone()
                    && gmarketRunnableFuture.isDone() && streetRunnableFuture.isDone()) {
                Pool.getInstance().getBehavior().get(0).setList(coupangTree);
                Pool.getInstance().getBehavior().get(1).setList(gmarketTree);
                Pool.getInstance().getBehavior().get(2).setList(streetTree);

                System.out.println("Data loading complete - product name: " + productName + "\n" + LINE);

                break;
            }
        }

        return Pool.getInstance().getBehavior();
    }

    private static void loadSettor(ResultSet resultSet, HashMap<String, String> branches,
                                   HashMap<Integer, HashMap<String, String>> tree) throws SQLException {
        branches.put("productName", resultSet.getString("title"));
        branches.put("productHref", resultSet.getString("link"));
        branches.put("productPrice", resultSet.getString("price"));
        branches.put("productImageSrc", resultSet.getString("src"));
        branches.put("productRatingStar", resultSet.getString("ratingstar"));
        branches.put("productRatingCount", resultSet.getString("ratingcount"));

        tree.put(resultSet.getInt("ranking"), branches);
    }
}
