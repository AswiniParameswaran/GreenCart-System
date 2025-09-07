import React, { useState } from "react";
import { useNavigate } from "react-router-dom";
import DOMPurify from "dompurify"; 
import ApiService from "../../service/ApiService";
import '../../style/register.css';

const RegisterPage = () => {
    const [formData, setFormData] = useState({
        email: '',
        name: '',
        phoneNumber: '',
        password: ''
    });

    const [message, setMessage] = useState(null);
    const navigate = useNavigate();

    const handleChange = (e) => {
        const { name, value } = e.target;
        
        const safeValue = DOMPurify.sanitize(value);
        setFormData({ ...formData, [name]: safeValue });
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        try {
            const response = await ApiService.registerUser(formData);
            if (response.status === 200) {
                setMessage("User Successfully Registered");

                
                setTimeout(() => {
                    navigate("/login");
                }, 1000);
            } else {
                setMessage("Unexpected response. Please try again.");
            }
        } catch (error) {
            
            const safeErrorMsg = error.response?.data?.message
                ? DOMPurify.sanitize(error.response.data.message)
                : "Unable to register. Please try again.";
            setMessage(safeErrorMsg);

    
            await new Promise(resolve => setTimeout(resolve, 1000));
        }
    };

    return (
        <div className="register-page">
            <h2>Register</h2>
            
            {message && (
                <p
                    className="message"
                    dangerouslySetInnerHTML={{ __html: DOMPurify.sanitize(message) }}
                />
            )}
            <form onSubmit={handleSubmit} autoComplete="off">
                <label>Email: </label>
                <input
                    type="email"
                    name="email"
                    value={formData.email}
                    onChange={handleChange}
                    required
                />

                <label>Name: </label>
                <input
                    type="text"
                    name="name"
                    value={formData.name}
                    onChange={handleChange}
                    required
                />

                <label>Phone Number: </label>
                <input
                    type="tel"
                    name="phoneNumber"
                    pattern="[0-9]{10,15}" 
                    value={formData.phoneNumber}
                    onChange={handleChange}
                    required
                />

                <label>Password: </label>
                <input
                    type="password"
                    name="password"
                    value={formData.password}
                    onChange={handleChange}
                    required
                    autoComplete="new-password" 
                />

                <button type="submit">Register</button>
                <p className="register-link">
                    Already have an account? <a href="/login">Login</a>
                </p>
            </form>
        </div>
    );
};

export default RegisterPage;
