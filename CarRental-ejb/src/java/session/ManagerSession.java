package session;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import rental.Car;
import rental.CarRentalCompany;
import rental.CarType;
import rental.Reservation;

@Stateless
public class ManagerSession implements ManagerSessionRemote {
    
    @PersistenceContext EntityManager em;
    
    @Override
    public Set<CarType> getCarTypes(String company) {
        try {
            return new HashSet<CarType>(em.find(CarRentalCompany.class, company).getAllTypes());
        } catch (IllegalArgumentException ex) {
            Logger.getLogger(ManagerSession.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
    }

    @Override
    public Set<Integer> getCarIds(String company, String type) {
        Set<Integer> out = new HashSet<Integer>();
        try {
            for(Car c: em.find(CarRentalCompany.class, company).getCars(type)){
                out.add(c.getId());
            }
        } catch (IllegalArgumentException ex) {
            Logger.getLogger(ManagerSession.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
        return out;
    }

    @Override
    public int getNumberOfReservations(String company, String type, int id) {
        try {
            return em.find(CarRentalCompany.class, company).getCar(id).getReservations().size();
        } catch (IllegalArgumentException ex) {
            Logger.getLogger(ManagerSession.class.getName()).log(Level.SEVERE, null, ex);
            return 0;
        }
    }

    @Override
    public int getNumberOfReservations(String company, String type) {
        Set<Reservation> out = new HashSet<Reservation>();
        try {
            for(Car c: em.find(CarRentalCompany.class, company).getCars(type)){
                out.addAll(c.getReservations());
            }
        } catch (IllegalArgumentException ex) {
            Logger.getLogger(ManagerSession.class.getName()).log(Level.SEVERE, null, ex);
            return 0;
        }
        return out.size();
    }

    @Override
    public int getNumberOfReservationsBy(String renter) {
        /*Set<Reservation> out = new HashSet<Reservation>();
        for(String crcString : this.getAllRentalCompanies()) {
            CarRentalCompany crc = em.find(CarRentalCompany.class, crcString);
            out.addAll(crc.getReservationsBy(renter));
        }
        return out.size(); */

        return em.createQuery(
                "SELECT COUNT(r) "
                + "FROM Reservation r "
                + "WHERE r.carRenter = :name").setParameter("name", renter).getFirstResult();
    }
    
    public List<String> getAllRentalCompanies(){
        return em.createQuery(
                "SELECT c.name "
              + "FROM CarRentalCompany c").getResultList();
    }
    
    public List<String> getAllCarTypesFromCompany(String companyName){
        return  em.createQuery(
                "SELECT t.name "
                + "FROM CarType t, CarRentalCompany c "
                + "WHERE t.name = :name"
        ).setParameter("name", companyName).getResultList();
    }
    
    public void addNewCompany(String name) {
        CarRentalCompany company = new CarRentalCompany(name, null);
        em.persist(company);
    }
    
    public void addNewCarType(String name, int nbOfSeats, float trunkSpace, double rentalPricePerDay, boolean smokingAllowed, String companyName){
        CarType carType = em.find(CarType.class, name);
        if(carType == null){
            CarType type = new CarType(name, nbOfSeats, trunkSpace, rentalPricePerDay, smokingAllowed);
            em.find(CarRentalCompany.class, companyName).addCarType(type);
        }
        else{
            em.find(CarRentalCompany.class, companyName).addCarType(carType);
        }
    }
    
    public void addNewCar(int id, String type, String companyName){
        CarType carType = em.find(CarType.class, type);
        Car car = new Car(id, carType);
        CarRentalCompany company = em.find(CarRentalCompany.class, companyName);
        company.addCar(car);
    }

    private CarRentalCompany loadRental(String name, String datafile) {
        CarRentalCompany company = null;
        Logger.getLogger(ManagerSession.class.getName()).log(Level.INFO, "loading {0} from file {1}", new Object[]{name, datafile});
        try {
            List<Car> cars = loadData(datafile);
            company = new CarRentalCompany(name, cars);
        } catch (NumberFormatException ex) {
            Logger.getLogger(ManagerSession.class.getName()).log(Level.SEVERE, "bad file", ex);
        } catch (IOException ex) {
            Logger.getLogger(ManagerSession.class.getName()).log(Level.SEVERE, null, ex);
        }
        return company;
    }

    private List<Car> loadData(String datafile) throws NumberFormatException, IOException {

        List<Car> cars = new LinkedList<Car>();

        int nextuid = 0;

        //open file from jar
        BufferedReader in = new BufferedReader(new InputStreamReader(ManagerSession.class.getClassLoader().getResourceAsStream(datafile)));
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
            CarType type = new CarType(csvReader.nextToken(),
                    Integer.parseInt(csvReader.nextToken()),
                    Float.parseFloat(csvReader.nextToken()),
                    Double.parseDouble(csvReader.nextToken()),
                    Boolean.parseBoolean(csvReader.nextToken()));
            //create N new cars with given type, where N is the 5th field
            for (int i = Integer.parseInt(csvReader.nextToken()); i > 0; i--) {
                cars.add(new Car(nextuid++, type));
            }
        }

        return cars;
    }

    @Override
    public String getMostPopularCarRentalCompany() {
        String popularCompany = "";
        int nbReservations = 0;

        for (String companyString : this.getAllRentalCompanies()) {

            CarRentalCompany company = em.find(CarRentalCompany.class, companyString);

            int reservationCount = company.getTotalNbReservations();
            if (reservationCount > nbReservations) {
                nbReservations = reservationCount;
                popularCompany = companyString;
            }

        }
        return popularCompany;
    }
}