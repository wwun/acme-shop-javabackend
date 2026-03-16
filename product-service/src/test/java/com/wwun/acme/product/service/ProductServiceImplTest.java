package com.wwun.acme.product.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;

import com.wwun.acme.product.dto.ProductCreateRequestDTO;
import com.wwun.acme.product.entity.Category;
import com.wwun.acme.product.entity.Product;
import com.wwun.acme.product.exception.CategoryNotFoundException;
import com.wwun.acme.product.mapper.ProductMapper;
import com.wwun.acme.product.repository.CategoryRepository;
import com.wwun.acme.product.repository.ProductRepository;

@SpringBootTest
public class ProductServiceImplTest 
{
	@Mock ProductRepository productRepository;
	@Mock CategoryRepository categoryRepository;
	@Mock ProductMapper productMapper;

	@InjectMocks ProductServiceImpl productServiceImpl;

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

	//save_shouldThrow_whenCategoryNotFound
	//delete_shouldDeleteProduct_whenProductExists
	//delete_shouldThrow_whenProductDoesNotExist
	//increaseStock_shouldIncreaseStockAndSaveMovemen
	//decreaseStock_shouldThrow_whenInsufficientStock
	//updateStock_shouldThrow_whenAmountNegative
}