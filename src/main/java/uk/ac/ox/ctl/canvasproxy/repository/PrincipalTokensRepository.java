package uk.ac.ox.ctl.canvasproxy.repository;

import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.CrudRepository;
import uk.ac.ox.ctl.canvasproxy.model.PrincipalTokens;

import javax.persistence.LockModeType;
import javax.persistence.QueryHint;
import java.util.Optional;

public interface PrincipalTokensRepository extends CrudRepository<PrincipalTokens, String> {

    /**
     * This is used to obtain a lock on a principal's tokens so that we can prevent multiple threads from updating
     * the token at the same time.
     * @param principal The principal to lock the tokens for.
     * @return An Optional PrincipalTokens object.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from PrincipalTokens p where p.principal = ?1")
    @QueryHints({@QueryHint(name = "javax.persistence.lock.timeout", value = "10000")})
    Optional<PrincipalTokens> lockById(String principal);

}
