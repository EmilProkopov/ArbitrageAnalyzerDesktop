import com.course_project.arbitrage_analyzer.desktop.ViewImpl;
import com.course_project.arbitrage_analyzer.interfaces.ArbitrageView;

public class Main {

    public static void main(String[] args) {
        ////////////////////////////////////////////////////////////
        args = new String[]{"/home/emmanuil/Documents/diplom/resLogs/resLog1.csv", "60", "BTC/UST", "50", "true", "true", "true", "true", "20", "0.5", "5", "1000000", "Simple"};
        ///////////////////////////////////////////////////////////
        ArbitrageView view = new ViewImpl(args);
    }
}
