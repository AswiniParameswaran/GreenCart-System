package com.example.shopBackend.service;

import com.example.shopBackend.dto.AddressDto;
import com.example.shopBackend.dto.Response;
import com.example.shopBackend.entity.Address;
import com.example.shopBackend.entity.User;
import com.example.shopBackend.enums.UserRole;
import com.example.shopBackend.exceptions.NotFoundException;
import com.example.shopBackend.mapper.EntityDtoMapper;
import com.example.shopBackend.repository.AddressRepo;
import com.example.shopBackend.security.XssSanitizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import jakarta.validation.ValidationException;


@Service
@RequiredArgsConstructor
@Slf4j
public class AddressServiceImpl implements AddressService {
    @Autowired
    private AddressRepo addressRepo;
    @Autowired
    private UserService userService;
    @Autowired
    private XssSanitizer xssSanitizer;

    private static final int MAX_STREET_LENGTH = 200;
    private static final int MAX_CITY_LENGTH = 100;
    private static final int MAX_STATE_LENGTH = 100;
    private static final int MAX_COUNTRY_LENGTH = 100;
    private static final int MAX_ZIP_LENGTH = 20;

    @Override
    public Response saveAndUpdateAddress(AddressDto addressDto) {

        User user = userService.getLoginUser();
        if (user == null) {
            throw new NotFoundException("Authenticated user not found");
        }


        if (addressDto == null) {
            throw new ValidationException("Address data is required");
        }

        validateAndSanitize(addressDto);

        Address address = user.getAddress();

        boolean preExisting = (address != null);

        if (address == null) {
            address = new Address();
            address.setUser(user);
        }

        if (addressDto.getStreet() != null) address.setStreet(xssSanitizer.sanitize(addressDto.getStreet()));
        if (addressDto.getCity() != null) address.setCity(xssSanitizer.sanitize(addressDto.getCity()));
        if (addressDto.getState() != null) address.setState(xssSanitizer.sanitize(addressDto.getState()));
        if (addressDto.getZipCode() != null) address.setZipCode(xssSanitizer.sanitize(addressDto.getZipCode()));
        if (addressDto.getCountry() != null) address.setCountry(xssSanitizer.sanitize(addressDto.getCountry()));

        addressRepo.save(address);

        String message = preExisting ? "Address successfully updated" : "Address successfully created";
        return Response.builder()
                .status(200)
                .message(message)
                .build();
    }

    private void validateAndSanitize(AddressDto dto){
        if (dto.getStreet() != null && dto.getStreet().length() > MAX_STREET_LENGTH) {
            throw new ValidationException("Street is too long");
        }
        if (dto.getCity() != null && dto.getCity().length() > MAX_CITY_LENGTH) {
            throw new ValidationException("City is too long");
        }
        if (dto.getState() != null && dto.getState().length() > MAX_STATE_LENGTH) {
            throw new ValidationException("State is too long");
        }
        if (dto.getCountry() != null && dto.getCountry().length() > MAX_COUNTRY_LENGTH) {
            throw new ValidationException("Country is too long");
        }
        if (dto.getZipCode() != null && dto.getZipCode().length() > MAX_ZIP_LENGTH) {
            throw new ValidationException("Zip code is too long");
        }

        if (dto.getStreet() != null) dto.setStreet(StringUtils.trimWhitespace(dto.getStreet()));
        if (dto.getCity() != null) dto.setCity(StringUtils.trimWhitespace(dto.getCity()));
        if (dto.getState() != null) dto.setState(StringUtils.trimWhitespace(dto.getState()));
        if (dto.getCountry() != null) dto.setCountry(StringUtils.trimWhitespace(dto.getCountry()));
        if (dto.getZipCode() != null) dto.setZipCode(StringUtils.trimWhitespace(dto.getZipCode()));
    }
}
