package hanane.sid.Billservice;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.data.rest.core.config.Projection;
import org.springframework.hateoas.PagedModel;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import javax.persistence.*;
import java.util.Collection;
import java.util.Date;


@Entity
@Data @NoArgsConstructor @AllArgsConstructor
class Bill {

	@Id @GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	private Date dateF;
	@JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
	private Long customerId;

	@Transient//n'est pas pris en consideration au niveau d la BD
	private Customer customer;

	@OneToMany(mappedBy = "bill")
	private Collection<ProductItem> productItems;

}
//------------------------------------------------------------------------
@RepositoryRestResource
interface BillRepository extends JpaRepository<Bill,Long>{

}
//------------------------------------------------------------------------
@Projection(name = "fullBill",types = Bill.class)
interface BillProjetion{
public Long getId();
public Date getDateF();
public Long getCustomerId();
public Collection<ProductItem> getProductItems();
}
/*****************************************************************************************************/
@Entity
@Data @AllArgsConstructor @NoArgsConstructor
class ProductItem{

	@Id @GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	@JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
	private Long productID;
	private double price;
	private double quantity;
	@Transient
	private Product product;

	@ManyToOne
	@JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
	//pr eviter la boucle infinie, on demande a spring de ne pas deserialiser l'obje BILL quand on deserialise
	//ProductItem , on peut utiliser JsonIgnore,mais cela va l'ppliquer soi en ecriture ou lecture
	private Bill bill;
}
//--------------------------------------------------------------------------------
@RepositoryRestResource
interface ProductItemRepository extends JpaRepository<ProductItem,Long>{

}
/*****************************************************************************************************/
@Data
class Customer{
	private Long id;
	private String name;
	private String email;

}
/*****************************************************************************************************/
@FeignClient(name = "CUSTOMER-SERVICE")
interface CustomerService{

	@GetMapping("/customers/{id}")
	public Customer findCustomerById(@PathVariable(name = "id") Long id);
}

/*****************************************************************************************************/
@Data
class Product{
	private String name;
	private double price;
	private Long id;
}
//------------------------------------------------------------------------------------
@FeignClient(name = "INVENTORY-SERVICE")
interface InventoryService{
	@GetMapping("/products/{id}")
	public Product findProductById(@PathVariable(name = "id") Long id);

	@GetMapping("/products")
	public PagedModel<Product> findAllProducts();
	//PagedModel:permet de deserialiser data json vers data pagine a l'aide du framework hateoas, content pr recuperer
	// les differents donnees particulier
}
/*****************************************************************************************************/
@RestController
class BillRestController{
	@Autowired
	private BillRepository billRepository;
	@Autowired
	private ProductItemRepository productItemRepository;
	@Autowired
	private CustomerService customerService;
	@Autowired
	private InventoryService inventoryService;

	@GetMapping("/fullBill/{id}")
	public Bill fullBill(@PathVariable(name = "id") Long idB){
		Bill bill = billRepository.findById(idB).get();
		bill.setCustomer(customerService.findCustomerById(bill.getCustomerId()));
		productItemRepository.findAll().forEach(pi->{
			pi.setProduct(inventoryService.findProductById(pi.getProductID()));
		});
		return  bill;
	}
}


@SpringBootApplication
@EnableFeignClients//pour conneter le service billing avec les autres services
public class BillServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(BillServiceApplication.class, args);
	}

	@Bean
	CommandLineRunner start(BillRepository billRepository, ProductItemRepository productItemRepository,
							CustomerService customerService,
							InventoryService inventoryService) {
		return args -> {
			Customer c1 = customerService.findCustomerById( 1L);
			System.out.println("------------------------------");
			System.out.println("id: "+c1.getId());
			System.out.println("name: "+c1.getName());
			Bill bill = new Bill(null, new Date(), c1.getId(), c1, null);
			billRepository.save(bill);
			PagedModel<Product> products = inventoryService.findAllProducts();
			products.getContent().forEach(p ->{
				productItemRepository.save(new ProductItem(null, p.getId(), p.getPrice(), 6, p, bill));
			});

		};
	}
}

