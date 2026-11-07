package com.anuhcollections.backend.address;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/addresses")
public class AddressController {

    private final AddressService addressService;

    public AddressController(AddressService addressService) {
        this.addressService = addressService;
    }

    @GetMapping
    public List<AddressResponse> getAddresses(
            Authentication authentication) {
        return addressService.getAddresses(authentication.getName());
    }

    @GetMapping("/{id}")
    public AddressResponse getAddress(
            Authentication authentication,
            @PathVariable Long id) {
        return addressService.getAddress(authentication.getName(), id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AddressResponse createAddress(
            Authentication authentication,
            @Valid @RequestBody AddressRequest request) {
        return addressService.createAddress(
                authentication.getName(), request);
    }

    @PutMapping("/{id}")
    public AddressResponse updateAddress(
            Authentication authentication,
            @PathVariable Long id,
            @Valid @RequestBody AddressRequest request) {
        return addressService.updateAddress(
                authentication.getName(), id, request);
    }

    @PatchMapping("/{id}/default")
    public AddressResponse setDefaultAddress(
            Authentication authentication,
            @PathVariable Long id) {
        return addressService.setDefaultAddress(
                authentication.getName(), id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAddress(
            Authentication authentication,
            @PathVariable Long id) {
        addressService.deleteAddress(authentication.getName(), id);
    }
}