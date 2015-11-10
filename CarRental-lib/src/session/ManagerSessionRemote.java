package session;

import java.util.List;
import java.util.Set;
import javax.ejb.Remote;
import rental.CarType;
import rental.Reservation;

@Remote
public interface ManagerSessionRemote {
    
    public Set<CarType> getCarTypes(String company);
    
    public Set<Integer> getCarIds(String company,String type);
    
    public int getNumberOfReservations(String company, String type, int carId);
    
    public int getNumberOfReservations(String company, String type);
      
    public int getNumberOfReservationsBy(String renter);
    
    public void addNewCompany(String name);
    
    public List<String> getAllRentalCompanies();

    public String getMostPopularCarRentalCompany();
    
    public void addNewCarType(String name, int nbOfSeats, float trunkSpace, double rentalPricePerDay, boolean smokingAllowed, String companyName);
    
    public void addNewCar(int id, String type, String companyName);
}