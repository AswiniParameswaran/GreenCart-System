import React, { useState, useEffect } from "react";
import { useNavigate, useLocation } from "react-router-dom";
import ApiService from "../../service/ApiService";
import DOMPurify from "dompurify"; // For XSS sanitization
import '../../style/address.css';

const AddressPage = () => {

    const [address, setAddress] = useState({
        street: '',
        city: '',
        state: '',
        zipCode: '',
        country: ''
    });

    const [error, setError] = useState(null);
    const navigate = useNavigate();
    const location = useLocation();

    // Fetch user address for edit
    useEffect(() => {
        if (location.pathname === '/edit-address') {
            fetchUserInfo();
        }
    }, [location.pathname]);

    const fetchUserInfo = async () => {
        try {
            const response = await ApiService.getLoggedInUserInfo();
            // Validate response structure to prevent broken access control
            if (response?.user?.address) {
                setAddress({
                    street: DOMPurify.sanitize(response.user.address.street || ''),
                    city: DOMPurify.sanitize(response.user.address.city || ''),
                    state: DOMPurify.sanitize(response.user.address.state || ''),
                    zipCode: DOMPurify.sanitize(response.user.address.zipCode || ''),
                    country: DOMPurify.sanitize(response.user.address.country || '')
                });
            }
        } catch (error) {
            setError(error.response?.data?.message || error.message || "Unable to fetch user information");
        }
    };

    const handleChange = (e) => {
        const { name, value } = e.target;
        // Sanitize input immediately to prevent XSS
        const sanitizedValue = DOMPurify.sanitize(value);
        setAddress((prevAddress) => ({
            ...prevAddress,
            [name]: sanitizedValue
        }));
    };

    const handSubmit = async (e) => {
        e.preventDefault();
        try {
            // Backend should validate the address and prevent unauthorized updates
            await ApiService.saveAddress(address);
            navigate("/profile");
        } catch (error) {
            setError(error.response?.data?.message || error.message || "Failed to save/update address");
        }
    };

    return (
        <div className="address-page">
            <h2>{location.pathname === '/edit-address' ? 'Edit Address' : "Add Address"}</h2>
            {error && <p className="error-message">{error}</p>}

            <form onSubmit={handSubmit}>
                <label>
                    Street:
                    <input
                        type="text"
                        name="street"
                        value={address.street}
                        onChange={handleChange}
                        required
                        autoComplete="off"
                    />
                </label>
                <label>
                    City:
                    <input
                        type="text"
                        name="city"
                        value={address.city}
                        onChange={handleChange}
                        required
                        autoComplete="off"
                    />
                </label>
                <label>
                    State:
                    <input
                        type="text"
                        name="state"
                        value={address.state}
                        onChange={handleChange}
                        required
                        autoComplete="off"
                    />
                </label>
                <label>
                    Zip Code:
                    <input
                        type="text"
                        name="zipCode"
                        value={address.zipCode}
                        onChange={handleChange}
                        required
                        autoComplete="off"
                    />
                </label>
                <label>
                    Country:
                    <input
                        type="text"
                        name="country"
                        value={address.country}
                        onChange={handleChange}
                        required
                        autoComplete="off"
                    />
                </label>
                <button type="submit">{location.pathname === '/edit-address' ? 'Edit Address' : "Save Address"}</button>
            </form>
        </div>
    );
};

export default AddressPage;
