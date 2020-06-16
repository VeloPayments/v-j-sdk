package example.certificates;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Stream;

/**
 * A demonstration of creating and storing blockchain certificates.
 * <ul>
 * <li> creating a blockchain with a local connection</li>
 * <li> creating a simple certificate that contains two fields (a String and a Long)</li>
 * <li> storing the certificate in the blockchain</li>
 * <li> retrieving the certificate from the blockchain and reading the fields.</li>
 * </ul>
 */
public class SimpleCertificate {

    private static final int NUMBER_OF_CONCURRENT_THREADS = 1;
    private static final int THREAD_POLL_SIZE = 20;
    private static final int SUBMIT_INTERVAL_IN_MS = 500;

    /**
     * Notes for Main:
     * <pre>
     * (1) - Defines the entity creating transactions using a unique id along with encryption and signing key pairs
     * (2) - ....
     * (3) - ...
     * </pre>
     *
     * @param args the command line arguments
     * @throws Exception is there is an unhandled error
     */
    public static void main(String[] args) throws Exception {
        Integer repeat = Stream.of(args).filter(s -> s.length() > 7 && s.startsWith("repeat="))
            .map(s -> Integer.parseInt(s.substring(7).trim()))
            .findFirst().orElse(1);
        SimpleCertificateRunnable simpleCertificateRunnable = new SimpleCertificateRunnable(repeat);
        ExecutorService executorService = Executors.newFixedThreadPool(THREAD_POLL_SIZE);
        Map<Integer, Future<?>> futures = new HashMap<>();
        for (int i = 0; i < NUMBER_OF_CONCURRENT_THREADS; i++) {
            Future<?> submit = executorService.submit(simpleCertificateRunnable);
            futures.put(i, submit);
            Thread.sleep(SUBMIT_INTERVAL_IN_MS);
        }

        futures.forEach((index, future) -> {
            try {
                future.get();
                System.out.println(String.format("%s --- %s", index, future.isDone()));
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        });
        executorService.shutdown();
    }


}
