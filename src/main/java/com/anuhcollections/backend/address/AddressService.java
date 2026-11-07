package com.anuhcollections.backend.address;

import java.util.Comparator;
import java.util.List;

import com.anuhcollections.backend.user.User;
import com.anuhcollections.backend.user.UserRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class AddressService {

    private final AddressRepository addressRepository;
    private final UserRepository userRepository;

    public AddressService(
            AddressRepository addressRepository,
            UserRepository userRepository) {
        this.addressRepository = addressRepository;
        this.userRepository = userRepository;
    }

    public List<AddressResponse> getAddresses(String email) {
        User user = getUser(email);

        return addressRepository.findAllByUser_Id(user.getId()).stream()
                .sorted(
                        Comparator.comparing(Address::isDefaultAddress)
                                .reversed()
                                .thenComparing(Address::getId))
                .map(this::toResponse)
                .toList();
    }

    public AddressResponse getAddress(String email, Long addressId) {
        User user = getUser(email);
        return toResponse(findOwnedAddress(addressId, user.getId()));
    }

    @Transactional
    public AddressResponse createAddress(
            String email,
            AddressRequest request) {
        User user = getUser(email);

        boolean makeDefault = request.defaultAddress()
                || !addressRepository.existsByUser_Id(user.getId());

        if (makeDefault) {
            clearDefaultAddress(user.getId());
        }

        Address address = new Address();
        address.setUser(user);
        applyRequest(address, request);
        address.setCountry("India");
        address.setDefaultAddress(makeDefault);

        return toResponse(addressRepository.save(address));
    }

    @Transactional
    public AddressResponse updateAddress(
            String email,
            Long addressId,
            AddressRequest request) {
        User user = getUser(email);
        Address address = findOwnedAddress(addressId, user.getId());

        if (request.defaultAddress()) {
            clearDefaultAddress(user.getId());
        }

        applyRequest(address, request);
        address.setCountry("India");
        address.setDefaultAddress(request.defaultAddress());

        return toResponse(addressRepository.save(address));
    }

    @Transactional
    public AddressResponse setDefaultAddress(
            String email,
            Long addressId) {
        User user = getUser(email);
        Address address = findOwnedAddress(addressId, user.getId());

        clearDefaultAddress(user.getId());
        address.setDefaultAddress(true);

        return toResponse(addressRepository.save(address));
    }

    @Transactional
    public void deleteAddress(String email, Long addressId) {
        User user = getUser(email);
        Address address = findOwnedAddress(addressId, user.getId());
        boolean wasDefault = address.isDefaultAddress();

        addressRepository.delete(address);

        if (wasDefault) {
            addressRepository
                    .findFirstByUser_IdOrderByIdAsc(user.getId())
                    .ifPresent(nextAddress -> {
                        nextAddress.setDefaultAddress(true);
                        addressRepository.save(nextAddress);
                    });
        }
    }

    private void clearDefaultAddress(Long userId) {
        addressRepository.findAllByUser_Id(userId).stream()
                .filter(Address::isDefaultAddress)
                .forEach(address -> address.setDefaultAddress(false));
    }

    private User getUser(String email) {
        return userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Account not found"));
    }

    private Address findOwnedAddress(Long addressId, Long userId) {
        return addressRepository.findByIdAndUser_Id(addressId, userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Address not found"));
    }

    private void applyRequest(
            Address address,
            AddressRequest request) {
        address.setFullName(request.fullName().trim());
        address.setPhone(request.phone().trim());
        address.setAddressLine1(request.addressLine1().trim());
        address.setAddressLine2(cleanOptional(request.addressLine2()));
        address.setLandmark(cleanOptional(request.landmark()));
        address.setCity(request.city().trim());
        address.setState(request.state().trim());
        address.setPostalCode(request.postalCode().trim());
    }

    private String cleanOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private AddressResponse toResponse(Address address) {
        return new AddressResponse(
                address.getId(),
                address.getFullName(),
                address.getPhone(),
                address.getAddressLine1(),
                address.getAddressLine2(),
                address.getLandmark(),
                address.getCity(),
                address.getState(),
                address.getPostalCode(),
                address.getCountry(),
                address.isDefaultAddress());
    }
}