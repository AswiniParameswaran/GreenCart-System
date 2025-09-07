import React, { useState } from "react";
import '../../style/navbar.css';
import { NavLink, useNavigate } from "react-router-dom";
import ApiService from "../../service/ApiService";
import DOMPurify from "dompurify"; 

const Navbar = () => {

    const [searchValue, setSearchValue] = useState("");
    const navigate = useNavigate();

    const isAdmin = ApiService.isAdmin();
    const isAuthenticated = ApiService.isAuthenticated();

    
    const handleSearchChange = (e) => {
        
        const sanitizedValue = DOMPurify.sanitize(e.target.value);
        setSearchValue(sanitizedValue);
    }

    
    const handleSearchSubmit = async (e) => {
        e.preventDefault();
        
        const query = encodeURIComponent(searchValue);
        navigate(`/?search=${query}`);
    }

    
    const handleLogout = () => {
        const confirmLogout = window.confirm("Are you sure you want to logout?");
        if (confirmLogout) {
            ApiService.logout(); 
            setTimeout(() => {
                navigate('/login');
            }, 500);
        }
    }

    return (
        <nav className="navbar">
            <div className="navbar-brand">
                <NavLink to="/" > <img src="/flower.webp" alt="Flower Mart" /> </NavLink>
            </div>

            
            <form className="navbar-search" onSubmit={handleSearchSubmit}>
                <input
                    type="text"
                    placeholder="Search products"
                    value={searchValue}
                    onChange={handleSearchChange}
                    autoComplete="off" 
                />
                <button type="submit">Search</button>
            </form>

            <div className="navbar-link">
                <NavLink to="/" >Home</NavLink>
                <NavLink to="/categories" >Categories</NavLink>
                {isAuthenticated && <NavLink to="/profile" >My Account</NavLink>}
                {isAdmin && <NavLink to="/admin" >Admin</NavLink>}
                {!isAuthenticated && <NavLink to="/login" >Login</NavLink>}
                {isAuthenticated && <NavLink onClick={handleLogout} >Logout</NavLink>}
                <NavLink to="/cart">Cart</NavLink>
            </div>
        </nav>
    );
};

export default Navbar;
