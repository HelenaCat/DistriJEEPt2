package client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.naming.InitialContext;
import rental.CarType;
import rental.Reservation;
import rental.ReservationConstraints;
import session.CarRentalSessionRemote;
import session.ManagerSessionRemote;

public class Main extends AbstractTestManagement<CarRentalSessionRemote, ManagerSessionRemote> {

    public Main(String scriptFile) {
        super(scriptFile);
    }

    public static void main(String[] args) throws Exception {
        // TODO: use updated manager interface to load cars into companies
        Main main = new Main("trips");
        ManagerSessionRemote session = main.getNewManagerSession("jefke", "jefke cars");
        main.loadRental("Dockx", "dockx.csv", session);
        main.loadRental("Hertz", "hertz.csv", session);
        main.run();
    }

    public void addNewCompany(String name, String datafile, ManagerSessionRemote session) {
        session.addNewCompany(name);
    }

    @Override
    protected CarRentalSessionRemote getNewReservationSession(String name) throws Exception {
        CarRentalSessionRemote out = (CarRentalSessionRemote) new InitialContext().lookup(CarRentalSessionRemote.class.getName());
        out.setRenterName(name);
        return out;
    }

    @Override
    protected ManagerSessionRemote getNewManagerSession(String name, String carRentalName) throws Exception {
        ManagerSessionRemote out = (ManagerSessionRemote) new InitialContext().lookup(ManagerSessionRemote.class.getName());
        return out;
    }

    @Override
    protected void checkForAvailableCarTypes(CarRentalSessionRemote session, Date start, Date end) throws Exception {
        System.out.println("Available car types between " + start + " and " + end + ":");
        for (CarType ct : session.getAvailableCarTypes(start, end)) {
            System.out.println("\t" + ct.toString());
        }
        System.out.println();
    }

    @Override
    protected void addQuoteToSession(CarRentalSessionRemote session, String name, Date start, Date end, String carType, String carRentalName) throws Exception {
        session.createQuote(carRentalName, new ReservationConstraints(start, end, carType));
    }

    @Override
    protected List<Reservation> confirmQuotes(CarRentalSessionRemote session, String name) throws Exception {
        return session.confirmQuotes();
    }

    @Override
    protected int getNumberOfReservationsBy(ManagerSessionRemote ms, String renterName) throws Exception {
        return ms.getNumberOfReservationsBy(renterName);
    }

    @Override
    protected int getNumberOfReservationsForCarType(ManagerSessionRemote ms, String name, String carType) throws Exception {
        return ms.getNumberOfReservations(name, carType);
    }

    @Override
    protected String getCheapestCarType(CarRentalSessionRemote session, Date start, Date end) throws Exception {
        return session.getCheapestCarType(start, end);
    }

    @Override
    protected String getMostPopularCarRentalCompany(ManagerSessionRemote ms) throws Exception {
        return ms.getMostPopularCarRentalCompany();
    }

    private void loadRental(String name, String datafile, ManagerSessionRemote session) {
        Logger.getLogger(Main.class.getName()).log(Level.INFO, "loading {0} from file {1}", new Object[]{name, datafile});
        try {
            session.addNewCompany(name);
            this.loadData(datafile, session, name);
        } catch (NumberFormatException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, "bad file", ex);
        } catch (IOException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void loadData(String datafile, ManagerSessionRemote session, String companyName) throws NumberFormatException, IOException {
        int nextuid = 0;

        //open file from jar
        BufferedReader in = new BufferedReader(new InputStreamReader(Main.class.getClassLoader().getResourceAsStream(datafile)));
        //while next line exists
        while (in.ready()) {
            //read line
            String line = in.readLine();
            //if comment: skip
            if (line.startsWith("#")) {
                continue;
            }
            //tokenize on ,
            StringTokenizer csvReader = new StringTokenizer(line, ",");
            //create new car type from first 5 fields
            String typeName = csvReader.nextToken();
            session.addNewCarType(typeName,
                    Integer.parseInt(csvReader.nextToken()),
                    Float.parseFloat(csvReader.nextToken()),
                    Double.parseDouble(csvReader.nextToken()),
                    Boolean.parseBoolean(csvReader.nextToken()), companyName);
            //create N new cars with given type, where N is the 5th field
            for (int i = Integer.parseInt(csvReader.nextToken()); i > 0; i--) {
                session.addNewCar(nextuid++, typeName, companyName);
            }
        }

    }
}
