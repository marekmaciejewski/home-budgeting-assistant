package pl.mm.homebudget.persistence;

import pl.mm.homebudget.persistence.entity.Register;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RegisterRepository extends ReactiveCrudRepository<Register, String> {
}
