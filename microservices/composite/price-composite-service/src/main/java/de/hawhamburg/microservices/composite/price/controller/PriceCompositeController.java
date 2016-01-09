package de.hawhamburg.microservices.composite.price.controller;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.parsing.Parser;
import de.hawhamburg.microservices.composite.price.model.CalculatedPrice;
import de.hawhamburg.microservices.composite.price.model.Price;
import de.hawhamburg.microservices.composite.price.service.PriceCompositeIntegration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import se.callista.microservices.util.ServiceUtils;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import java.net.URI;
import java.util.UUID;

import static com.jayway.restassured.RestAssured.given;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

/**
 * Created by unknown on 27.10.15.
 */

@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@RestController
public class PriceCompositeController {

    private static final Logger LOG = LoggerFactory.getLogger(PriceCompositeController.class);

    @Autowired
    private PriceCompositeIntegration priceCompositeIntegration;

    @Autowired
    private ServiceUtils utils;

    @Autowired
    private RestTemplate restTemplate;

    @RequestMapping(value = "/price/{flightId}", method = RequestMethod.GET)
    public ResponseEntity<CalculatedPrice> getPrice(@PathVariable final UUID flightId){
        ResponseEntity<Price> priceResult = priceCompositeIntegration.getPrice(flightId);
        if(!priceResult.getStatusCode().is2xxSuccessful()){
            Price price = priceCompositeIntegration.calculatePriceForFlight(flightId);
//            TODO
//            price composite integration anschauen, revenue composite greift auf price zu, auch anschauen
//            calculate from flightop api
//            new price
//            priceCompositeIntegration.createPrice()
//                    return priceCompositeIntegration
//            core service anschauen
            if (price.equals(null)) {
                return utils.createResponse(null,priceResult.getStatusCode());
            }
            return utils.createOkResponse(new CalculatedPrice(price));
        }
        return utils.createOkResponse(new CalculatedPrice(priceResult.getBody()));
    }


    @RequestMapping(value = "/price", method = RequestMethod.POST)
    public ResponseEntity<CalculatedPrice> createPrice(@RequestBody final Price price){
        ResponseEntity<Price> priceResult = priceCompositeIntegration.createPrice(price);
        if(!priceResult.getStatusCode().is2xxSuccessful()){
            return utils.createResponse(null,priceResult.getStatusCode());
        }
        return utils.createOkResponse(new CalculatedPrice(priceResult.getBody()));
    }

    @RequestMapping(value = "/price/{flightID}", method = RequestMethod.DELETE)
    public void deletePrice(@RequestBody final UUID flightID, HttpServletResponse response){
        try{
            priceCompositeIntegration.deletePrice(flightID);
            response.setStatus(HttpServletResponse.SC_OK);
        } catch (Exception e){
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    @RequestMapping(value = "/price", method = RequestMethod.PUT)
    public void putPrice(@RequestBody final Price price,HttpServletResponse response){
        try {
            response.setStatus(HttpServletResponse.SC_OK);
            priceCompositeIntegration.putPrice(price);
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    @RequestMapping(value="/testPrice/{flightId}" , method = RequestMethod.GET)
    public CalculatedPrice calc(@PathVariable final UUID flightId){
        LOG.debug("got a request to testPrice");
        CalculatedPrice price = null;
        URI uri = utils.getServiceUrl("priceapi");
        String url = uri.toString() + "/" + flightId;

        //OF TODO remove this later!
//        String urlToPriceService = "http://localhost:8080";
//        String url =urlToPriceService + "/price/" + flightID;

        LOG.debug("url to api is : " + url);
        String token = getToken();
        LOG.debug("token is " + token);
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<CalculatedPrice> resultStr = null;
        try {
            resultStr = restTemplate.exchange(uri, HttpMethod.GET, entity, CalculatedPrice.class);
            price = resultStr.getBody();
        } catch (RestClientException e) {
            LOG.error(e.getMessage());
        }
        return price;
    }

    private String getToken(){

        javax.net.ssl.HttpsURLConnection.setDefaultHostnameVerifier(
                new javax.net.ssl.HostnameVerifier() {
                    public boolean verify(String hostname,
                                          javax.net.ssl.SSLSession sslSession) {
                        return true;
                    }
                });


        String baseAddress = "127.0.0.1";
        String token;
        RestAssured.useRelaxedHTTPSValidation();
        // Register JSON Parser for plain text responses
        RestAssured.registerParser("text/plain", Parser.JSON);
        // Order and extract
        token =
                given().
                        param("grant_type", "password").
                        param("client_id", "acme").
                        param("username", "user").
                        param("password", "password").
                        when().post("https://acme:acmesecret@" + baseAddress + ":9999/uaa/oauth/token").then().
                        extract().path("access_token");
        return token;
    }
}
