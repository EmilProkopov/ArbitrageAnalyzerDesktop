import com.course_project.arbitrage_analyzer.desktop.ViewImpl;
import com.course_project.arbitrage_analyzer.interfaces.ArbitrageView;

public class Main {

    public static void main(String[] args) {
        ////////////////////////////////////////////////////////////
        // args = new String[]{"D:\\resLogs\\resLog1_1.csv", "10", "BTC/USD", "10", "true", "true", "true", "true", "3", "0.5", "-1", "-1", "BayesLaplace"};
        ///////////////////////////////////////////////////////////
        ArbitrageView view = new ViewImpl(args);
    }
}
