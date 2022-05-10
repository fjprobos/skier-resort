import io.swagger.client.ApiClient;
import io.swagger.client.ApiException;
import io.swagger.client.ApiResponse;
import io.swagger.client.api.ResortsApi;
import io.swagger.client.api.SkiersApi;
import io.swagger.client.model.LiftRide;
import io.swagger.client.model.ResortIDSeasonsBody;
/*import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;*/

import java.io.FileNotFoundException;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.io.PrintWriter;

public class DriverClass {
    // upgrade EC2
    // threads and request number
    // client -> EC2
    static int numThreads = 0;
    static int numSkiers = 0;
    static int numLifts = 40;
    static int numRuns = 10;
    static String url = null;
    //static List<Data> data = new ArrayList<>();
    static List<LatencyRecord> records = Collections.synchronizedList(new ArrayList<LatencyRecord>());
    static StringBuilder sb = new StringBuilder();
    final private static int MAX_RETRY = 5;
    final private static String FILE_NAME = "res/Data1.xlsx";
    static int successfulRequests = 0;
    static int unsuccessfulRequests = 0;
    static String type = "skier";

    private static int getRandom(int start, int end) {
        Random random = new Random();
        return random.nextInt(end) + start + 1;
    }

    private static void sendSkierPostRequest(int requests, int startSkierId, int endSkierId, int startLiftRideTime,
                                             int endLiftRideTime, String phase, CountDownLatch cdl) {
        SkiersApi api = new SkiersApi();
        ApiClient client = api.getApiClient();
        client.setBasePath(url);

        for (int i = 0; i < requests; i++) {
            Integer resortId = 1;
            String seasonId = "1";
            String dayId = "1";
            Integer skierId = getRandom(startSkierId, endSkierId);
            LiftRide liftRide = new LiftRide(getRandom(startLiftRideTime, endLiftRideTime), getRandom(0, 100), getRandom(0, 10));

            int count = 0;
            while (count <= MAX_RETRY) {
                try {
                    ApiResponse response = api.writeNewLiftRideWithHttpInfo(liftRide, resortId, seasonId, dayId, skierId);
                    Long startTime = System.currentTimeMillis();
                    api.writeNewLiftRide(liftRide, resortId, seasonId, dayId, skierId);
                    Long endTime = System.currentTimeMillis();
                    LatencyRecord obs = new LatencyRecord("Skier Post", phase, requests, client.toString(),
                            i, startTime, endTime, endTime - startTime);
                    records.add(obs);
                    //data.add(new Data(startTime, endTime, endTime - startTime, "POST", response.getStatusCode()));
                    successfulRequests++;
                    //System.out.println(String.format("Request sent = %d ms", endTime - startTime));
                    break;
                } catch (ApiException e) {
                    count++;
                    unsuccessfulRequests++;
                }
            }
        }
        cdl.countDown();
    }

    private static void sendResortPostRequest(int requests, int startSkierId, int endSkierId, int startLiftRideTime,
                                              int endLiftRideTime, String phase, CountDownLatch cdl) {

        ResortsApi api = new ResortsApi();
        ApiClient client = api.getApiClient();
        client.setBasePath(url);

        for (int i = 0; i < requests; i++) {
            Integer resortId = 1;
            int year = getRandom(0,100000000);
            ResortIDSeasonsBody body = new ResortIDSeasonsBody();
            body.year(Integer.toString(year));

            int count = 0;
            while (count <= MAX_RETRY) {
                try {
                    ApiResponse response = api.addSeasonWithHttpInfo(body, resortId);
                    Long startTime = System.currentTimeMillis();
                    api.addSeason(body, resortId);
                    Long endTime = System.currentTimeMillis();
                    LatencyRecord obs = new LatencyRecord("Resort Post", phase, requests, client.toString(),
                            i, startTime, endTime, endTime - startTime);
                    records.add(obs);
                    //data.add(new Data(startTime, endTime, endTime - startTime, "POST", response.getStatusCode()));
                    successfulRequests++;
                    //System.out.println(String.format("Request sent = %d ms", endTime - startTime));
                    break;
                } catch (ApiException e) {
                    count++;
                    unsuccessfulRequests++;
                }
            }
        }
        cdl.countDown();
    }

    private static void setEnvironmentParameters(String[] args) {
        try {
            numThreads = Integer.parseInt(args[0]);
            numSkiers = Integer.parseInt(args[1]);
            if (args.length == 3) {
                url = args[2];
            } else if (args.length == 4) {
                numLifts = Integer.parseInt(args[2]);
                url = args[3];
            } else {
                numLifts = Integer.parseInt(args[2]);
                numRuns = Integer.parseInt(args[3]);
                url = args[4];
            }
        } catch (NumberFormatException e) {
            System.out.println("Invalid argument passed to the parameters list");
        }
    }

    private static void startPhase1(CountDownLatch generalCDL) throws InterruptedException {
        final int threads = numThreads / 4;
        final int skierIDs = numSkiers / threads;
        final int requests = ((int) (numRuns * 0.2)) * skierIDs;
        CountDownLatch countDownLatch = new CountDownLatch((int) (0.2 * threads));

        final long startTime = System.currentTimeMillis();
        for (int i = 0; i < threads; i++) {
            int nextValue = i + 1;
            final int startSkierId = i * skierIDs + 1;
            final int endSkierId = skierIDs * nextValue;
            Runnable thread = () -> {
                sendPostRequest(requests, startSkierId, endSkierId, 0 , 90, "Phase 1",
                        generalCDL);
                countDownLatch.countDown();
            };
            new Thread(thread).start();
        }
        countDownLatch.await();
        long endTime = System.currentTimeMillis();

        System.out.println(String.format("The time to execute the first phase is %d ms", endTime - startTime));
    }

    private static void startPhase2(CountDownLatch generalCDL) throws InterruptedException {
        final int threads = numThreads;
        final int skierIDs = numSkiers / threads;
        final int requests = ((int) (numRuns * 0.6)) * skierIDs;
        CountDownLatch countDownLatch = new CountDownLatch((int) (0.2 * threads));

        final long startTime = System.currentTimeMillis();
        for (int i = 0; i < threads; i++) {
            //System.out.println("Post");
            int nextValue = i + 1;
            final int startSkierId = i * skierIDs + 1;
            final int endSkierId = skierIDs * nextValue;
            Runnable thread = () -> {
                sendPostRequest(requests, startSkierId, endSkierId, 91, 360, "Phase 2",
                        generalCDL);
                countDownLatch.countDown();
            };
            new Thread(thread).start();
        }
        countDownLatch.await();
        long endTime = System.currentTimeMillis();

        System.out.println(String.format("The time to execute the second phase is %d ms", endTime - startTime));
    }

    private static void startPhase3(CountDownLatch generalCDL) throws InterruptedException {
        final int threads = (int) (0.1 * numThreads);
        final int skierIDs = numSkiers / threads;
        final int requests = (int) (numRuns * 0.1);
        CountDownLatch countDownLatch = new CountDownLatch(threads);

        final long startTime = System.currentTimeMillis();
        for (int i = 0; i < threads; i++) {
            int nextValue = i + 1;
            final int startSkierId = i * skierIDs + 1;
            final int endSkierId = skierIDs * nextValue;
            Runnable thread = () -> {
                sendPostRequest(requests, startSkierId, endSkierId, 361 , 420, "Phase 3",
                        generalCDL);
                countDownLatch.countDown();
            };
            new Thread(thread).start();
        }
        countDownLatch.await();
        long endTime = System.currentTimeMillis();

        System.out.println(String.format("The time to execute the third phase is %d ms", endTime - startTime));
    }

    private static void showDetails(long wallTime) {
        System.out.println(String.format("\nThe number of successful requests is %d", successfulRequests));
        System.out.println(String.format("The number of unsuccessful requests is %d\n", unsuccessfulRequests));
    }

    private static void sendPostRequest(int requests, int startSkierId, int endSkierId, int startLiftRideTime,
                                        int endLiftRideTime, String phase, CountDownLatch cdl) {
        if (type.equals("skier")) {
            sendSkierPostRequest(requests, startSkierId, endSkierId, startLiftRideTime,
                    endLiftRideTime, phase, cdl);
        }
        else if (type.equals("resort")) {
            sendResortPostRequest(requests, startSkierId, endSkierId, startLiftRideTime,
                    endLiftRideTime, phase, cdl);
        }
        else if (type.equals("both")) {
            sendSkierPostRequest(requests, startSkierId, endSkierId, startLiftRideTime,
                    endLiftRideTime, phase, cdl);
            sendResortPostRequest(requests, startSkierId, endSkierId, startLiftRideTime,
                    endLiftRideTime, phase, cdl);
        }
    }
/*
    private static void fillData() throws IOException {
        Workbook workbook = new XSSFWorkbook();

        Sheet sheet = workbook.createSheet("Data");

        int rowCount = 0;
        Row header = sheet.createRow(rowCount);
        Cell headerCell = header.createCell(0);
        headerCell.setCellValue("Start Time");
        headerCell = header.createCell(1);
        headerCell.setCellValue("End Time");
        headerCell = header.createCell(2);
        headerCell.setCellValue("Latency (in ms)");
        headerCell = header.createCell(3);
        headerCell.setCellValue("Request Type");
        headerCell = header.createCell(4);
        headerCell.setCellValue("Response Code");

        for (Data d : data) {
            rowCount++;
            Row row = sheet.createRow(rowCount);
            Cell cell = row.createCell(0);
            cell.setCellValue(d.getStartTime());
            cell = row.createCell(1);
            cell.setCellValue(d.getEndTime());
            cell = row.createCell(2);
            cell.setCellValue(d.getLatency());
            cell = row.createCell(3);
            cell.setCellValue(d.getRequestType());
            cell = row.createCell(4);
            cell.setCellValue(d.getResponseCode());
        }

        FileOutputStream outputStream = new FileOutputStream(FILE_NAME);
        workbook.write(outputStream);
        workbook.close();
        System.out.println("The data has been exported to " + FILE_NAME);
    }

    private static void calculateDataSetResults(long wallTime) {
        Collections.sort(data);
        long totalLatency = 0L;
        long minimumResponseTime = Long.MAX_VALUE;
        long maximumResponseTime = Long.MIN_VALUE;
        for (Data d : data) {
            final long latency = d.getLatency();
            totalLatency += latency;
            if (latency > maximumResponseTime) maximumResponseTime = latency;
            if (latency < minimumResponseTime) minimumResponseTime = latency;
        }
        final long meanResponseTime = totalLatency / ( successfulRequests );
        final long medianResponseTime = (data.get(successfulRequests / 2).getLatency() + data.get(successfulRequests / 2 - 1).getLatency()) / 2;
        final float throughput = successfulRequests / (float) wallTime;

        System.out.println(String.format("\nThe findings from the data set collected are as follows:\n" +
                "1. Mean Response Time = %dms\n" +
                "2. Median Response Time = %dms\n" +
                "3. Minimum Response Time = %dms\n" +
                "4. Maximum Response Time = %dms\n" +
                "5. Throughput = %.2f\n" +
                "6. Wall Time = %dms", meanResponseTime, medianResponseTime, minimumResponseTime, maximumResponseTime, throughput, wallTime));
    }*/

    public static void main(String[] args) throws InterruptedException, FileNotFoundException {
        if (args.length >= 3 && args.length <= 5) {
            // start the csv header
            sb.append("task,");
            sb.append("phase,");
            sb.append("numberOfRequestsPerThread,");
            sb.append("threadId,");
            sb.append("requestId,");
            sb.append("start,");
            sb.append("end,");
            sb.append("duration");
            sb.append('\n');
            setEnvironmentParameters(args);

            int totalThreads = numThreads / 4 + numThreads + (int) (0.1 * numThreads);
            CountDownLatch generalCDL = new CountDownLatch(totalThreads);

            if (type.equals("both")) {
                generalCDL = new CountDownLatch(2 * totalThreads);
            }

            final long startTime = System.currentTimeMillis();
            startPhase1(generalCDL);
            startPhase2(generalCDL);
            startPhase3(generalCDL);

            // Wait until all threads are finished
            generalCDL.await();

            final long endTime = System.currentTimeMillis();

            long wallTime = endTime - startTime;
            showDetails(wallTime);

            // Pass concurrent list observations to string builder
            for (LatencyRecord obs : records) {

                sb.append(obs.getTask());
                sb.append(",");
                sb.append(obs.getPhase());
                sb.append(',');
                sb.append(obs.getRequestsPerThread());
                sb.append(',');
                sb.append(obs.getThreadID());
                sb.append(',');
                sb.append(obs.getRequestID());
                sb.append(',');
                sb.append(obs.getStart());
                sb.append(',');
                sb.append(obs.getEnd());
                sb.append(',');
                sb.append(obs.getDuration());
                sb.append('\n');
            }

            // Write data into csv
            try (PrintWriter writer = new PrintWriter("massiveUpload.csv")) {
                writer.write(sb.toString());
                writer.close();
            }
            catch (FileNotFoundException e) {
                System.out.println(e.getMessage());
            }

/*            try {
                fillData();
                calculateDataSetResults(endTime - startTime);
            } catch (IOException e) {
                System.out.println("There was error while saving or loading the data");
            }*/
        } else {
            System.out.println("Invalid arguments passed to the program");
        }
    }

}
