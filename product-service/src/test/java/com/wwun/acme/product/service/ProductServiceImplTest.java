package com.wwun.acme.product.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;

import com.wwun.acme.product.dto.ProductCreateRequestDTO;
import com.wwun.acme.product.dto.ProductResponseDTO;
import com.wwun.acme.product.entity.Category;
import com.wwun.acme.product.entity.Product;
import com.wwun.acme.product.entity.StockMovement;
import com.wwun.acme.product.enums.StockOperation;
import com.wwun.acme.product.exception.CategoryNotFoundException;
import com.wwun.acme.product.exception.InsufficientStockException;
import com.wwun.acme.product.exception.InvalidStockAmountException;
import com.wwun.acme.product.exception.ProductAlreadyExistsException;
import com.wwun.acme.product.exception.ProductNotFoundException;
import com.wwun.acme.product.mapper.ProductMapper;
import com.wwun.acme.product.metric.CacheMetrics;
import com.wwun.acme.product.repository.ProductRepository;
import com.wwun.acme.product.repository.StockMovementRepository;

@ExtendWith(MockitoExtension.class)
public class ProductServiceImplTest 
{
	@Mock ProductRepository productRepository;
	@Mock CategoryService categoryService;
	@Mock ProductMapper productMapper;
	@Mock StockMovementRepository stockMovementRepository;
	@Mock CacheMetrics cacheMetrics;

	@InjectMocks ProductServiceImpl productServiceImpl;

	@AfterEach
	void cleanUp(){
		SecurityContextHolder.clearContext();
	}

    @Test
	void findAll_shouldReturnAllProducts() {

		//Given
		List<Product> expected = List.of(new Product(), new Product());

		when(productRepository.findAll()).thenReturn(expected);

		//When
		List<Product> result = productServiceImpl.findAll();

		//Then
		assertSame(expected, result);

		verify(productRepository).findAll();

	}

	@Test
	void findById_shouldReturnProduct_whenProductExists(){

		//Given
		UUID productId = UUID.randomUUID();

		Product product = new Product();
		product.setId(productId);
		product.setName("cellphone");

		when(productRepository.findById(productId)).thenReturn(Optional.of(product));

		//When
		Product result = productServiceImpl.findById(productId);

		//Then
		assertTrue(result!=null);
		assertSame(product, result);
		assertEquals("cellphone", result.getName());

		verify(productRepository).findById(productId);
		
	}

	@Test
    void findById_shouldThrow_whenProductDoesNotExist() {

        // Given
        UUID productId = UUID.randomUUID();
        when(productRepository.findById(productId)).thenReturn(Optional.empty());
 
        // When
        ProductNotFoundException ex = assertThrows(ProductNotFoundException.class,
                () -> productServiceImpl.findById(productId));
 
        // Then
        assertTrue(ex.getMessage().contains(productId.toString()));
        verify(productRepository).findById(productId);
	
	}

	@Test
	void save_shouldPersistProduct_whenCategoryExists(){

		//Give
		ProductCreateRequestDTO productCreateRequestDTO = mock(ProductCreateRequestDTO.class);
		
		UUID categoryId = UUID.randomUUID();
		Category category = new Category();
		category.setId(categoryId);
		category.setName("household");

		when(productCreateRequestDTO.getCategoryId()).thenReturn(categoryId);

		when(categoryService.findById(categoryId)).thenReturn(category);

		UUID productId = UUID.randomUUID();
		Product product = new Product();
		product.setId(productId);
		product.setName("vacuum");
		product.setPrice(new BigDecimal("100.00"));
		product.setStock(2);
		
		when(productMapper.toEntity(productCreateRequestDTO)).thenReturn(product);
		when(productRepository.findByName(product.getName())).thenReturn(Optional.empty());
		when(productRepository.save(product)).thenReturn(product);

		//When
		Product result = productServiceImpl.save(productCreateRequestDTO);

		//Then
		assertSame(product, result);
		assertEquals("vacuum", result.getName());
		assertEquals(categoryId, result.getCategory().getId());

		verify(categoryService).findById(categoryId);
		verify(productMapper).toEntity(productCreateRequestDTO);
		verify(productRepository).findByName(product.getName());
		verify(productRepository).save(product);

	}

	@Test
	void save_shouldThrow_whenCategoryNotFound(){

		//Given
		ProductCreateRequestDTO productCreateRequestDTO = mock(ProductCreateRequestDTO.class);

		UUID categoryId = UUID.randomUUID();

		when(productCreateRequestDTO.getCategoryId()).thenReturn(categoryId);

		when(categoryService.findById(categoryId)).thenThrow(new CategoryNotFoundException("Category not found"));

		//When
		CategoryNotFoundException ex = assertThrows(CategoryNotFoundException.class, () -> productServiceImpl.save(productCreateRequestDTO));

		//Then
		assertEquals("Category not found", ex.getMessage());

    	verify(categoryService).findById(categoryId);
		verify(productRepository, never()).save(any(Product.class));

	}

	@Test
    void save_shouldThrow_whenProductNameAlreadyExists() {
		
        // Given
        ProductCreateRequestDTO productCreateRequestDTO = mock(ProductCreateRequestDTO.class);
 
        UUID categoryId = UUID.randomUUID();
        Category category = new Category();
        category.setId(categoryId);
 
        when(productCreateRequestDTO.getCategoryId()).thenReturn(categoryId);
        when(categoryService.findById(categoryId)).thenReturn(category);
 
        Product existing = new Product();
        existing.setName("vacuum");
 
        Product mapped = new Product();
        mapped.setName("vacuum");
 
        when(productMapper.toEntity(productCreateRequestDTO)).thenReturn(mapped);
        when(productRepository.findByName("vacuum")).thenReturn(Optional.of(existing));
 
        // When
        ProductAlreadyExistsException ex = assertThrows(ProductAlreadyExistsException.class,
                () -> productServiceImpl.save(productCreateRequestDTO));
 
        // Then
        assertTrue(ex.getMessage().contains("vacuum"));
        verify(productRepository).findByName("vacuum");
        verify(productRepository, never()).save(any(Product.class));
	
	}

	@Test
	void delete_shouldDeleteProduct_whenProductExists(){

		//Given
		UUID productId = UUID.randomUUID();
		when(productRepository.existsById(productId)).thenReturn(true);

		//When
		productServiceImpl.delete(productId);

		//Then
		verify(productRepository).existsById(productId);
		verify(productRepository).deleteById(productId);

	}

	@Test
	void increaseStock_shouldIncreaseStockAndSaveMovement_whenProductExistsAndAmountIsPositive(){

		//Given
		UUID productId = UUID.randomUUID();

		int amount = 20;

		Product product = new Product();
		product.setId(productId);
		product.setStock(10);

		when(productRepository.findById(productId)).thenReturn(Optional.of(product));

		when(productRepository.save(any(Product.class))).thenAnswer(invocationArg -> invocationArg.getArgument(0));

		//When
		Optional<Product> result = productServiceImpl.increaseStock(productId, amount);

		//Then
		assertTrue(result.isPresent());
		assertEquals(productId, result.get().getId());
		assertEquals(30, result.get().getStock());

		verify(productRepository).findById(productId);
		verify(productRepository).save(product);
		
		ArgumentCaptor<StockMovement> captor = ArgumentCaptor.forClass(StockMovement.class);
		verify(stockMovementRepository).save(captor.capture());

		StockMovement savedMovement = captor.getValue();

		assertEquals(amount, savedMovement.getQuantity());
		assertEquals(StockOperation.INCREASE, savedMovement.getOperation());
		assertSame(product, savedMovement.getProduct());

	}

	@Test
	void delete_shouldThrow_whenProductDoesNotExist(){

		//Given
		UUID productId = UUID.randomUUID();

		when(productRepository.existsById(productId)).thenReturn(false);

		//When
		ProductNotFoundException ex = assertThrows(ProductNotFoundException.class, () -> productServiceImpl.delete(productId));

		//Then
		assertTrue(ex.getMessage().toLowerCase().contains("product not found with id"));
		assertTrue(ex.getMessage().contains(productId.toString()));

		verify(productRepository).existsById(productId);
		verify(productRepository, never()).deleteById(any(UUID.class));

	}

	@Test
	void increaseStock_shouldThrow_whenAmountIsZeroOrNegative(){

		//Given
		UUID productId = UUID.randomUUID();

		int amount = -1;

		//When
		InvalidStockAmountException ex = assertThrows(InvalidStockAmountException.class, () -> productServiceImpl.increaseStock(productId, amount));

		//Then
		assertTrue(ex.getMessage().toLowerCase().contains("increase amount must be positive"));
		
		verify(productRepository, never()).findById(any(UUID.class));
		verify(productRepository, never()).save(any(Product.class));
		verify(stockMovementRepository, never()).save(any(StockMovement.class));

	}

	@Test
	void decreaseStock_shouldThrow_whenInsufficientStock(){

		//Given
		UUID productId = UUID.randomUUID();

		int amount = 100;

		Product product = new Product();
		product.setId(productId);
		product.setStock(10);

		when(productRepository.findById(productId)).thenReturn(Optional.of(product));

		//When
		InsufficientStockException ex = assertThrows(InsufficientStockException.class, () -> productServiceImpl.decreaseStock(productId, amount));

		//Then
		assertTrue(ex.getMessage().toLowerCase().contains("insufficient stock"));
		assertTrue(ex.getMessage().contains(productId.toString()));
		
		verify(productRepository).findById(productId);
		verify(productRepository, never()).save(any(Product.class));
		verify(stockMovementRepository, never()).save(any(StockMovement.class));

	}

	@Test
	void decreaseStock_shouldDecreaseStockAndSaveMovement_whenStockIsEnough() {

		// Given
		UUID productId = UUID.randomUUID();
		int amount = 5;

		Product product = new Product();
		product.setId(productId);
		product.setStock(10);

		when(productRepository.findById(productId)).thenReturn(Optional.of(product));
		when(productRepository.save(any(Product.class)))
				.thenAnswer(inv -> inv.getArgument(0));

		// When
		Optional<Product> result = productServiceImpl.decreaseStock(productId, amount);

		// Then
		assertTrue(result.isPresent());
		assertEquals(productId, result.get().getId());
		assertEquals(5, result.get().getStock());

		verify(productRepository).findById(productId);
		verify(productRepository).save(product);

		ArgumentCaptor<StockMovement> captor = ArgumentCaptor.forClass(StockMovement.class);
		verify(stockMovementRepository).save(captor.capture());

		StockMovement savedMovement = captor.getValue();

		assertEquals(amount, savedMovement.getQuantity());
		assertEquals(StockOperation.DECREASE, savedMovement.getOperation());
		assertSame(product, savedMovement.getProduct());
	}
	
	@Test
	void updateStock_shouldSetStockAndSaveMovement_whenAmountIsValid() {

		// Given
		UUID productId = UUID.randomUUID();
		int amount = 25;

		Product product = new Product();
		product.setId(productId);
		product.setStock(10);

		when(productRepository.findById(productId)).thenReturn(Optional.of(product));
		when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

		// When
		Optional<Product> result = productServiceImpl.updateStock(productId, amount);

		// Then
		assertTrue(result.isPresent());
		assertEquals(productId, result.get().getId());
		assertEquals(25, result.get().getStock());

		verify(productRepository).findById(productId);
		verify(productRepository).save(product);

		ArgumentCaptor<StockMovement> captor = ArgumentCaptor.forClass(StockMovement.class);
		verify(stockMovementRepository).save(captor.capture());

		StockMovement savedMovement = captor.getValue();

		assertEquals(amount, savedMovement.getQuantity());
		assertEquals(StockOperation.SET, savedMovement.getOperation());
		assertSame(product, savedMovement.getProduct());
	}

	@Test
    void getAllById_shouldThrow_whenSomeProductsNotFound() {
        // Given
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        List<UUID> ids = List.of(id1, id2);
 
        Product product = new Product();
        product.setId(id1);
 
        when(productRepository.findAllById(ids)).thenReturn(List.of(product));
 
        // When
        ProductNotFoundException ex = assertThrows(ProductNotFoundException.class,
                () -> productServiceImpl.getAllById(ids));
 
        // Then
        assertTrue(ex.getMessage().toLowerCase().contains("one or more products not found"));
        verify(productRepository).findAllById(ids);
    }
 
    @Test
    void getAllById_shouldReturnAll_whenAllProductsExist() {
        // Given
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        List<UUID> ids = List.of(id1, id2);
 
        Product p1 = new Product();
        p1.setId(id1);
        Product p2 = new Product();
        p2.setId(id2);
 
        when(productRepository.findAllById(ids)).thenReturn(List.of(p1, p2));
        when(productMapper.toResponseDTO(any(Product.class)))
                .thenAnswer(inv -> {
					Product p = inv.getArgument(0);
					return new ProductResponseDTO(
						p.getId(),
						p.getName(),
						p.getDescription(),
						p.getPrice(),
						p.getStock(),
						null
					);
				});
 
        // When
        List<ProductResponseDTO> result = productServiceImpl.getAllById(ids);
 
        // Then
        assertEquals(2, result.size());
        verify(productRepository).findAllById(ids);
	
	}
	
}