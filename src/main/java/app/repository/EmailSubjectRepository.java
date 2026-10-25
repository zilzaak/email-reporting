package app.repository;


import app.entity.EmailSubject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EmailSubjectRepository extends JpaRepository<EmailSubject,Long> {

    boolean existsByRootThreadId(String roothThreadId);

}
