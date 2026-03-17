package com.wwun.acme.product.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.context.SecurityContextHolder;

import com.wwun.acme.product.dto.ProductCreateRequestDTO;
import com.wwun.acme.product.entity.Category;
import com.wwun.acme.product.entity.Product;
import com.wwun.acme.product.entity.StockMovement;
import com.wwun.acme.product.enums.StockOperation;
import com.wwun.acme.product.exception.CategoryNotFoundException;
import com.wwun.acme.product.exception.InvalidStockAmountException;
import com.wwun.acme.product.exception.ProductNotFoundException;
import com.wwun.acme.product.mapper.ProductMapper;
import com.wwun.acme.product.repository.CategoryRepository;
import com.wwun.acme.product.repository.ProductRepository;
import com.wwun.acme.product.repository.StockMovementRepository;

@ExtendWith(MockitoExtension.class)
public class ProductServiceImplTest 
{
	@Mock ProductRepository productRepository;
	@Mock CategoryRepository categoryRepository;
	@Mock ProductMapper productMapper;
	@Mock StockMovementRepository stockMovementRepository;

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
		Optional<Product> result = productServiceImpl.findById(productId);

		//Then
		assertTrue(result.isPresent());
		assertSame(product, result.get());
		assertEquals("cellphone", result.get().getName());

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

		when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));

		UUID productId = UUID.randomUUID();
		Product product = new Product();
		product.setId(productId);
		product.setName("vacuum");
		product.setPrice(new BigDecimal("100.00"));
		product.setStock(2);
		
		when(productMapper.toEntity(productCreateRequestDTO)).thenReturn(product);

		when(productRepository.save(product)).thenReturn(product);

		//When
		Product result = productServiceImpl.save(productCreateRequestDTO);

		//Then
		assertSame(product, result);
		assertTrue(result.getName().equalsIgnoreCase("vacuum"));
		assertEquals(categoryId, result.getCategory().getId());

		verify(categoryRepository).findById(categoryId);
		verify(productMapper).toEntity(productCreateRequestDTO);
		verify(productRepository).save(product);

	}

	@Test
	void save_shouldThrow_whenCategoryNotFound(){

		//Given
		ProductCreateRequestDTO productCreateRequestDTO = mock(ProductCreateRequestDTO.class);

		UUID categoryId = UUID.randomUUID();

		when(productCreateRequestDTO.getCategoryId()).thenReturn(categoryId);

		when(categoryRepository.findById(categoryId)).thenReturn(Optional.empty());

		//When
		CategoryNotFoundException ex = assertThrows(CategoryNotFoundException.class, () -> productServiceImpl.save(productCreateRequestDTO));

		//Then
		assertEquals("Category not found", ex.getMessage());

    	verify(categoryRepository).findById(categoryId);

	}

	@Test
	void delete_shouldDeleteProduct_whenProductExists(){

		//Given
		UUID productId = UUID.randomUUID();
		when(productRepository.existsById(productId)).thenReturn(true);

		//When
		productServiceImpl.delete(productId);

		//Then
		verify(productRepository).findById(productId);
		verify(productRepository).deleteById(productId);

	}

	void increaseStock_shouldIncreaseStockAndSaveMovement_whenProductExistsAndAmountIsPositive(){

		// stockMovementRepository.save(
        //     new StockMovement(LocalDateTime.now(), amount, StockOperation.INCREASE, updated)
        // );

        // return Optional.of(updated

		//Given
		UUID productId = UUID.randomUUID();

		int amount = 20;	//duda para llenar el product.getstock()+amount

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
		verify(stockMovementRepository).save(any(StockMovement.class));

	}

	//delete_shouldThrow_whenProductDoesNotExist
	//increaseStock_shouldThrow_whenAmountIsZeroOrNegative
	//decreaseStock_shouldThrow_whenInsufficientStock
	//updateStock_shouldThrow_whenAmountNegative
}