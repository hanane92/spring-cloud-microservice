package hanane.sid.gatewayservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.discovery.ReactiveDiscoveryClient;
import org.springframework.cloud.gateway.discovery.DiscoveryClientRouteDefinitionLocator;
import org.springframework.cloud.gateway.discovery.DiscoveryLocatorProperties;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.cloud.netflix.hystrix.EnableHystrix;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

//this gateway based on spring cloud gateway+filtres
@SpringBootApplication
@EnableHystrix
public class GatewayServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(GatewayServiceApplication.class, args);
	}

	@Bean // this nethod remplace yml file, so if a request came with predicate "/customers/**" we will be
		// redirected to microservice wich address is(http://localhost:9090), we use this if w know the addresses

	RouteLocator staticRoutes(RouteLocatorBuilder routeLocatorBuilder){
		return routeLocatorBuilder.routes()
				.route(r->r
						.path("/countries/**")
						.filters(f->f
								.addRequestHeader("x-rapidapi-host","restcountries-v1.p.rapidapi.com")
								.addRequestHeader("x-rapidapi-key","149ac73582msh61881b438602ad7p15b1e1jsnc3604791724e")
								.rewritePath("/countries/(?<segment>.*)","/${segment}")
								.hystrix(h->h.setName("countries").setFallbackUri("forward:/defaultCountries"))//si on rencontre un prob en renvoie cette reponse par defaut de  hystrix

						)

						.uri("https://restcountries-v1.p.rapidapi.com/all").id("r1"))// ld:is load balancer

		        .route(r->r
				        .path("/salat/**")
				        .filters(f->f
								.addRequestHeader("x-rapidapi-host","muslimsalat.p.rapidapi.com")
								.addRequestHeader("x-rapidapi-key","149ac73582msh61881b438602ad7p15b1e1jsnc3604791724e")
								.rewritePath("/salat/(?<segment>.*)","/${segment}")

				)

				.uri("https://muslimsalat.p.rapidapi.com").id("r2")).build(); // ld:is load balancer



	}


	@Bean
	//cet objet (DiscoveryClientRouteDefinitionLocator) va contacter le service d'enregistrement [resoudre nom du service dynamiquement]
	//une fois il recoit une rquette verifie l'url recupere le nom du service puis contacte le service d'enreistrement
	//puis redirige requete vers micro concerne
	DiscoveryClientRouteDefinitionLocator dynamicRoutes(ReactiveDiscoveryClient rdc, DiscoveryLocatorProperties dlp){
		return new DiscoveryClientRouteDefinitionLocator(rdc,dlp);
	}

	@RestController
	class HystrixController {

		@GetMapping("/defaultCountries")
		public Map<String,String> countriesFallBack(){

			Map<String,String> countries = new HashMap<>();
			countries.put("MA","morocco");
			countries.put("USA","USA");
			return countries;

		}
	}



}
