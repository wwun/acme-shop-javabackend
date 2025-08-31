package com.wwun.acme.order.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.wwun.acme.order.dto.order.orderItem.OrderItemCreateRequestDTO;
import com.wwun.acme.order.dto.order.orderItem.OrderItemResponseDTO;
import com.wwun.acme.order.dto.order.orderItem.OrderItemUpdateRequestDTO;
import com.wwun.acme.order.dto.product.ProductResponseDTO;
import com.wwun.acme.order.entity.Order;
import com.wwun.acme.order.entity.OrderItem;
import com.wwun.acme.order.mapper.OrderItemMapper;
import com.wwun.acme.order.repository.OrderItemRepository;
import com.wwun.acme.order.repository.OrderRepository;
import com.wwun.acme.order.feign.ProductClient;

@Service
public class OrderItemServiceImpl implements OrderItemService{

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderItemMapper orderItemMapper;
    private final ProductClient productClient;

    public OrderItemServiceImpl(OrderRepository orderRepository, OrderItemRepository orderItemRepository, OrderItemMapper orderItemMapper, ProductClient productClient){
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.orderItemMapper = orderItemMapper;
        this.productClient = productClient;
    }

    @Override
    public OrderItem save(OrderItemCreateRequestDTO orderItemCreateRequestDTO) {

        Order order = orderRepository.findById(orderItemCreateRequestDTO.getOrderId()).orElseThrow(() -> new RuntimeException("Order not found"));

        OrderItem orderItem = orderItemMapper.toEntity(orderItemCreateRequestDTO);

        orderItem.setOrder(order);

        orderItem.setPriceAtPurchase(productClient.getProductPrice(orderItemCreateRequestDTO.getProductId()));

        return orderItemRepository.save(orderItem);

    }

    @Override
    public Optional<OrderItem> findById(UUID id) {
        return orderItemRepository.findById(id);
    }

    @Override
    public List<OrderItem> findAll() {
        return orderItemRepository.findAll();
    }

    @Override
    public List<OrderItemResponseDTO> findAllByOrderId(UUID orderId){
        
        List<OrderItem> orderItems = orderItemRepository.findAllByOrderId(orderId);

        List<UUID> productIds = orderItems.stream()
                .map(OrderItem::getProductId)
                .collect(Collectors.toList());

        List<ProductResponseDTO> products = productClient.getAllById(productIds);

        Map<UUID, ProductResponseDTO> productsMaps = products.stream()
                .collect(Collectors.toMap(ProductResponseDTO::getId, Function.identity()));

        return orderItems.stream()
                .map(orderItem -> {
                    OrderItemResponseDTO orderItemResponseDTO = orderItemMapper.toResponseDTO(orderItem);
                    orderItemResponseDTO.setProductName(productsMaps.get(orderItem.getProductId()).getName());
                    return orderItemResponseDTO;
                })
                .collect(Collectors.toList());

    }

    @Override
    public Optional<OrderItem> update(UUID id, OrderItemUpdateRequestDTO orderItemUpdateRequestDTO) {
        OrderItem existing = orderItemRepository.findById(id).orElseThrow(() -> new RuntimeException("OrderItem not found with id: " + id));
        
        //debo validar los datos qe se reciben de orderItemUpdateRequestDTO? considerando qe ya se hace validaciones en el mismo deto como con @NotNull @PositiveOrZero y otros
        //seria mejor usar un .map? aunque para esto tendria qe usan nuevamente un findById qe creo qe seria un golpe innecesario a la bd
        OrderItem orderItemUpdated = orderItemMapper.toEntity(orderItemUpdateRequestDTO);
        
        existing.setQuantity(orderItemUpdated.getQuantity());

        existing.setProductId(orderItemUpdated.getProductId());
        
        //creo qe orderId no va a ser actualizada, no creo qe tenga mucho sentido pasar un item a otro order, la idea seria qe se cree un orderItem nuevo en el respectivo order
        
        //este valor vuelve a hacer la busqueda del precio del producto
        existing.setPriceAtPurchase(BigDecimal.valueOf(100));  //wwun feign obtener desde microservicio product productCliente.getProductPrice(orderItemCreateRequestDTO.getProductId()) donde se hace la validacion si el product existe?
        
        return Optional.of(orderItemRepository.save(existing));

    }

    @Override
    public void delete(UUID id) {
        //se me ocurre qe es la unica validacion qe puedo hacer para saber qe se esta eliminando algo, validando qe se esta intenand eliminar un producto existente pero no se si es innecesario porqe finalmente si no existia el item al final ya no existira tampoco
        if(orderItemRepository.findById(id).isEmpty()){
            throw new RuntimeException("Item not found with id: " + id);
        }
        orderItemRepository.deleteById(id);
    }

}
