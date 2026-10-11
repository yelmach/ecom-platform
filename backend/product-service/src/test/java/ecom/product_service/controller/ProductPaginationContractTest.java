package ecom.product_service.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.stream.IntStream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import ecom.product_service.ProductServiceApplication;
import ecom.product_service.dto.response.ProductResponse;
import ecom.product_service.service.ProductService;

@WebMvcTest(controllers = ProductController.class, properties = "server.port=0")
@ContextConfiguration(classes = ProductServiceApplication.class)
class ProductPaginationContractTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private ProductService productService;

	// The application enables Mongo auditing; pagination needs no database.
	@MockitoBean(name = "mongoMappingContext")
	private MongoMappingContext mongoMappingContext;

	@ParameterizedTest
	@CsvSource({
			"/products, 0, 25, 3",
			"/products, 2, 25, 3",
			"/products, 0, 0, 0",
			"/products/me, 0, 25, 3",
			"/products/me, 2, 25, 3",
			"/products/me, 0, 0, 0"
	})
	void listsUseNestedPageMetadata(String endpoint, int number, long total, int totalPages) throws Exception {
		int count = (int) Math.min(10, Math.max(0, total - number * 10L));
		List<ProductResponse> content = IntStream.range(0, count)
				.mapToObj(index -> ProductResponse.builder().id("product-" + index).name("Keyboard").build())
				.toList();
		var page = new PageImpl<>(content, PageRequest.of(number, 10), total);
		when(productService.getProducts(number, 10)).thenReturn(page);
		when(productService.getProductsBySeller("seller-1", number, 10)).thenReturn(page);

		var result = mockMvc.perform(get(endpoint)
				.header("X-User-Id", "seller-1")
				.param("page", String.valueOf(number)).param("size", "10"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content").isArray())
				.andExpect(jsonPath("$.page.number").value(number))
				.andExpect(jsonPath("$.page.size").value(10))
				.andExpect(jsonPath("$.page.totalElements").value(total))
				.andExpect(jsonPath("$.page.totalPages").value(totalPages))
				.andExpect(jsonPath("$.totalElements").doesNotExist())
				.andExpect(jsonPath("$.last").doesNotExist())
				.andReturn();
		System.out.println(endpoint + " page " + number + ": " + result.getResponse().getContentAsString());
	}
}
