package accounts.client;

import common.money.Percentage;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import rewards.internal.account.Account;
import rewards.internal.account.Beneficiary;

import java.net.URI;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

public class AccountClientTests {

	private static final String BASE_URL = "http://localhost:8080";
	
	private RestTemplate restTemplate = new RestTemplate();
	private Random random = new Random();
	
	@Test
	public void listAccounts() {
		RestTemplate rst = new RestTemplate();
		String uri = "http://localhost:8080/accounts";

		// DONE: Run this test
		// - Remove the @Disabled on this test method.
		// - Then, use the restTemplate to retrieve an array containing all Account instances.
		// - Use BASE_URL to help define the URL you need: BASE_URL + "/..."
		// - Run the test and ensure that it passes.
		Account[] accounts = rst.getForObject(uri, Account[].class); // Modify this line to use the restTemplate
		
		assertNotNull(accounts);
		assertTrue(accounts.length >= 21);
		assertEquals("Keith and Keri Donald", accounts[0].getName());
		assertEquals(2, accounts[0].getBeneficiaries().size());
		assertEquals(Percentage.valueOf("50%"), accounts[0].getBeneficiary("Annabelle").getAllocationPercentage());
	}
	
	@Test
	public void getAccount() {
		RestTemplate rst = new RestTemplate();
		String uri = "http://localhost:8080/accounts/{id}";
		// DONE: Run this test
		// - Remove the @Disabled on this test method.
		// - Then, use the restTemplate to retrieve the Account with id 0 using a URI template
		// - Run the test and ensure that it passes.
		Account account = rst.getForObject(uri,Account.class,0); // Modify this line to use the restTemplate
		
		assertNotNull(account);
		assertEquals("Keith and Keri Donald", account.getName());
		assertEquals(2, account.getBeneficiaries().size());
		assertEquals(Percentage.valueOf("50%"), account.getBeneficiary("Annabelle").getAllocationPercentage());
	}
	
	@Test
	public void createAccount() {
		RestTemplate rst = new RestTemplate();
		String uri = "http://localhost:8080/accounts";
		// Use a unique number to avoid conflicts
		String number = String.format("12345%4d", random.nextInt(10000));
		Account account = new Account(number, "John Doe");
		account.addBeneficiary("Jane Doe");
		
		//	Done: Create a new Account
		//	- Remove the @Disabled on this test method.
		//	- Create a new Account by POSTing to the right URL and
		//    store its location in a variable
		//  - Note that 'RestTemplate' has two methods for this.
		//  - Use the one that returns the location of the newly created
		//    resource and assign that to a variable.
		URI newAccountLocation = rst.postForLocation(uri,account); // Modify this line to use the restTemplate

		//	Done: Retrieve the Account you just created from
		//	         the location that was returned.
		//	- Run this test, then. Make sure the test succeeds.
		Account retrievedAccount = rst.getForObject(newAccountLocation,Account.class); // Modify this line to use the restTemplate
		
		assertEquals(account.getNumber(), retrievedAccount.getNumber());
		
		Beneficiary accountBeneficiary = account.getBeneficiaries().iterator().next();
		Beneficiary retrievedAccountBeneficiary = retrievedAccount.getBeneficiaries().iterator().next();
		
		assertEquals(accountBeneficiary.getName(), retrievedAccountBeneficiary.getName());
		assertNotNull(retrievedAccount.getEntityId());
	}
	
	@Test
	public void addAndDeleteBeneficiary() {
		// perform both add and delete to avoid issues with side effects
		RestTemplate rst = new RestTemplate();
		String uri = "http://localhost:8080/accounts/{accountId}/beneficiaries";
		// DONE: Create a new Beneficiary
		// - Remove the @Disabled on this test method.
		// - Create a new Beneficiary called "David" for the account with id 1
		//	 (POST the String "David" to the "/accounts/{accountId}/beneficiaries" URL).
		// - Store the returned location URI in a variable.
		URI uriB = rst.postForLocation(uri,"David",1);

		// DONE: Retrieve the Beneficiary you just created from the location that was returned
		Beneficiary newBeneficiary =  rst.getForObject(uriB, Beneficiary.class);// Modify this line to use the restTemplate
		
		assertNotNull(newBeneficiary);
		assertEquals("David", newBeneficiary.getName());
		
		// DONE: Delete the newly created Beneficiary

		rst.delete(uriB);

		HttpClientErrorException httpClientErrorException = assertThrows(HttpClientErrorException.class, () -> {
			System.out.println("You SHOULD get the exception \"No such beneficiary with name 'David'\" in the server.");

			// DONE: Try to retrieve the newly created Beneficiary again.
			// - Run this test, then. It should pass because we expect a 404 Not Found
			//   If not, it is likely your delete in the previous step
			//   was not successful.
			rst.getForObject(uriB, Beneficiary.class);

		});
		assertEquals(HttpStatus.NOT_FOUND, httpClientErrorException.getStatusCode());
	}
	
}
