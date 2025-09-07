import React, { useState } from "react";
import { useNavigate } from "react-router-dom";
import DOMPurify from "dompurify"; 
import ApiService from "../../service/ApiService";
import '../../style/register.css';

const LoginPage = () => {
    const [formData, setFormData] = useState({
        email: '',
        password: ''
    });

    const [message, setMessage] = useState(null);
    const navigate = useNavigate();

    const handleAsgardeoLogin = () => {
        
        const asgardeoAuthURL =
            "https://api.asgardeo.io/t/org90t0t/oauth2/authorize?response_type=code&client_id=mjvpBT_GJegqW2ajrOkhj_RsmWAa&redirect_uri=http://localhost:3000/profile&scope=openid,profile";

        window.location.href = asgardeoAuthURL;
    };

    const handleChange = (e) => {
        const { name, value } = e.target;

        
        const safeValue = DOMPurify.sanitize(value);
        setFormData({ ...formData, [name]: safeValue });
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        try {
            const response = await ApiService.loginUser(formData);

            if (response.status === 200 && response.token) {
                setMessage("User Successfully Logged in");

               
                localStorage.setItem('app_token', response.token);
                localStorage.setItem('app_role', response.role);

                setTimeout(() => {
                    navigate("/profile");
                }, 1000);
            } else {
                setMessage("Invalid login response");
            }
        } catch (error) {
            
            const safeErrorMsg = error.response?.data?.message
                ? DOMPurify.sanitize(error.response.data.message)
                : "Unable to login. Please try again.";
            setMessage(safeErrorMsg);

           
            await new Promise(resolve => setTimeout(resolve, 1000));
        }
    };

    return (
        <div className="register-page">
            <h2>Login</h2>
           
            {message && <p
                className="message"
                dangerouslySetInnerHTML={{ __html: DOMPurify.sanitize(message) }}
            />}
            <form onSubmit={handleSubmit} autoComplete="off">
                <label>Email: </label>
                <input
                    type="email"
                    name="email"
                    value={formData.email}
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
                />

                <button type="submit">Login</button>
                <hr />
                <button
                    type="button"
                    onClick={handleAsgardeoLogin}
                    className="asgardeo-login-btn"
                >
                    Login with Asgardeo
                </button>

                <p className="register-link">
                    Don&apos;t have an account? <a href="/register">Register</a>
                </p>
            </form>
        </div>
    );
};

export default LoginPage;
