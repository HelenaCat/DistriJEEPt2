package session;

import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import javax.annotation.Resource;
import javax.ejb.EJBContext;
import javax.ejb.Stateful;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import rental.CarRentalCompany;
import rental.CarType;
import rental.Quote;
import rental.RentalStore;
import rental.Reservation;
import rental.ReservationConstraints;
import rental.ReservationException;

@Stateful
public class CarRentalSession implements CarRentalSessionRemote {

    @Resource
    private EJBContext context;
    
    @PersistenceContext EntityManager em;

    private String renter;
    private List<Quote> quotes = new LinkedList<Quote>();

    @Override
    public Set<String> getAllRentalCompanies() {
        List<String> list = em.createQuery(
                "SELECT c.name "
              + "FROM CarRentalCompany c").getResultList();
        Set<String> result = new HashSet<String>(list);
        return result;
    }
    
    @Override
    public List<CarType> getAvailableCarTypes(Date start, Date end) {
        List<CarType> availableCarTypes = new LinkedList<CarType>();
        for(String crc : getAllRentalCompanies()) {
            for(CarType ct : em.find(CarRentalCompany.class, crc).getAvailableCarTypes(start, end)) {
                if(!availableCarTypes.contains(ct))
                    availableCarTypes.add(ct);
            }
        }
        return availableCarTypes;
    }

    @Override
    public Quote createQuote(String company, ReservationConstraints constraints) throws ReservationException {
        try {
            Quote out = em.find(CarRentalCompany.class, company).createQuote(constraints, renter);
            quotes.add(out);
            return out;
        } catch(Exception e) {
            throw new ReservationException(e);
        }
    }

    @Override
    public List<Quote> getCurrentQuotes() {
        return quotes;
    }

    @Override
    public List<Reservation> confirmQuotes() throws ReservationException {
        List<Reservation> done = new LinkedList<Reservation>();
        try {
            for (Quote quote : quotes) {
               done.add(em.find(CarRentalCompany.class, quote.getRentalCompany()).confirmQuote(quote));
            }
        } catch (Exception e) {
            context.setRollbackOnly();
            //for(Reservation r:done)
            //    em.find(CarRentalCompany.class, r.getRentalCompany()).cancelReservation(r);
            throw new ReservationException(e);
        }
        return done;
    }

    @Override
    public void setRenterName(String name) {
        if (renter != null) {
            throw new IllegalStateException("name already set");
        }
        renter = name;
    }

    @Override
    public String getCheapestCarType(Date start, Date end) {
        CarType cheapestCarType = null;
        for (String companyString : this.getAllRentalCompanies()) {
            CarRentalCompany company = em.find(CarRentalCompany.class, companyString);

            CarType cheapestType = company.getCheapestCarType(start, end);
            if (cheapestCarType == null || cheapestType.getRentalPricePerDay() < cheapestCarType.getRentalPricePerDay()) {
                cheapestCarType = cheapestType;
            }

        }
        return cheapestCarType.getName();
    }
    
}