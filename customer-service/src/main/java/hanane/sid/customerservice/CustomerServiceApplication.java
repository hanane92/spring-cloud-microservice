package hanane.sid.customerservice;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.data.rest.core.config.Projection;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
class Customer{

	@Id @GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	private String name;
	private String email;
}

@RepositoryRestResource //web service RestFull de spring data
interface CustomerRepository extends JpaRepository<Customer,Long>{

}

@Projection(name = "p1",types = Customer.class) //[personalize elements to show] in this projection if client request a customer with projection 'p1' we return (id+name)
interface CustomerProjection{
   public Long getId();
   public String getName();
}

@SpringBootApplication
public class CustomerServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(CustomerServiceApplication.class, args);
	}

	 @Bean
	 CommandLineRunner start(CustomerRepository customerRepository, RepositoryRestConfiguration repositoryRestConfiguration){

		 return args->{

		 	 repositoryRestConfiguration.exposeIdsFor(Customer.class);//if we want show the id of class customer
		 	 customerRepository.save(new Customer(null,"ENSET","enset@gmail.com"));
             customerRepository.save(new Customer(null,"ENSAM","ensam@gmail.com"));
             customerRepository.save(new Customer(null,"EST","est@gmail.com"));
             customerRepository.findAll().forEach(System.out::println);
		 };
	 }

}
