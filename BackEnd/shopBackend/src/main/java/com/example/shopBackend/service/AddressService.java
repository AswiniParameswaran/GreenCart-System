package com.example.shopBackend.service;

import com.example.shopBackend.dto.AddressDto;
import com.example.shopBackend.dto.Response;

public interface AddressService {
    Response saveAndUpdateAddress(AddressDto addressDto);
}
