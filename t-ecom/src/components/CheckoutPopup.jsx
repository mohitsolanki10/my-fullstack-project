import axios from 'axios';
import React, { useContext, useState } from 'react';
import { Modal, Button, Form, Toast, ToastContainer } from 'react-bootstrap';
import { useNavigate } from 'react-router-dom';
import AppContext from '../Context/Context';

const CheckoutPopup = ({ show, handleClose, cartItems, totalPrice }) => {
  const baseUrl = import.meta.env.VITE_BASE_URL;
  const navigate = useNavigate();
  const { clearCart, refreshData } = useContext(AppContext);

  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [validated, setValidated] = useState(false);
  const [showToast, setShowToast] = useState(false);
  const [toastMessage, setToastMessage] = useState('');
  const [toastVariant, setToastVariant] = useState('success');
  const [isSubmitting, setIsSubmitting] = useState(false);

  const showMessage = (variant, message) => {
    setToastVariant(variant);
    setToastMessage(message);
    setShowToast(true);
  };

  // ---------------------------------------------------------------
  // STEP 3 (payment ho gaya): Razorpay ne jo diya wo backend ko bhejo.
  // Backend signature check karke order PLACED karta hai.
  // ---------------------------------------------------------------
  const verifyPayment = async (razorpayResponse) => {
    try {
      await axios.post(`${baseUrl}/api/payment/verify`, {
        razorpayOrderId: razorpayResponse.razorpay_order_id,
        razorpayPaymentId: razorpayResponse.razorpay_payment_id,
        razorpaySignature: razorpayResponse.razorpay_signature,
      });

      showMessage('success', 'Payment successful! Order placed.');
      clearCart();      // cart khali (state + localStorage dono)
      refreshData();    // stock ki nayi value laao
      setTimeout(() => navigate('/orders'), 2000);
    } catch (error) {
      console.log(error);
      const serverMsg = error.response?.data?.message;
      showMessage(
        'danger',
        `Payment received but order confirmation failed. ${serverMsg || ''} ` +
          `Please note your Payment ID: ${razorpayResponse.razorpay_payment_id}`
      );
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleConfirm = async (event) => {
    event.preventDefault();
    const form = event.currentTarget;

    if (form.checkValidity() === false) {
      event.stopPropagation();
      setValidated(true);
      return;
    }

    setValidated(true);
    setIsSubmitting(true);

    const orderItems = cartItems.map((item) => ({
      productId: item.id,
      quantity: item.quantity,
    }));

    const data = {
      customerName: name,
      email: email,
      items: orderItems,
    };

    try {
      // STEP 1: backend se Razorpay order banwao (amount backend khud nikalta hai)
      const { data: pay } = await axios.post(`${baseUrl}/api/payment/create-order`, data);

      if (!window.Razorpay) {
        showMessage('danger', 'Razorpay script load nahi hui. Internet check karke page refresh karo.');
        setIsSubmitting(false);
        return;
      }

      // STEP 2: Razorpay ka payment popup kholo
      const options = {
        key: pay.keyId,
        amount: pay.amount,
        currency: pay.currency,
        name: 'Telusko Store',
        description: `Order ${pay.orderId}`,
        order_id: pay.razorpayOrderId,
        prefill: { name: name, email: email },
        theme: { color: '#0d6efd' },
        handler: (response) => verifyPayment(response), // payment success
        modal: {
          ondismiss: () => setIsSubmitting(false),       // user ne popup band kar diya
        },
      };

      const razorpay = new window.Razorpay(options);
      razorpay.on('payment.failed', (response) => {
        showMessage('danger', `Payment failed: ${response.error.description}`);
        setIsSubmitting(false);
      });
      razorpay.open();
    } catch (error) {
      console.log(error);
      const serverMsg = error.response?.data?.message;
      showMessage('danger', serverMsg || 'Could not start payment. Please try again.');
      setIsSubmitting(false);
    }
  };

  return (
    <>
      {/* enforceFocus={false} zaroori hai, warna Razorpay popup me typing/OTP nahi chalta */}
      <Modal show={show} onHide={handleClose} centered enforceFocus={false}>
        <Modal.Header closeButton>
          <Modal.Title>Checkout</Modal.Title>
        </Modal.Header>
        <Form noValidate validated={validated} onSubmit={handleConfirm}>
          <Modal.Body>
            <div className="checkout-items mb-4">
              {cartItems.map((item) => (
                <div key={item.id} className="d-flex mb-3 border-bottom pb-3">
                  <img
                    src={`${baseUrl}/api/product/${item.id}/image`}
                    alt={item.name}
                    className="me-3 rounded"
                    style={{ width: '80px', height: '80px', objectFit: 'cover' }}
                  />
                  <div className="flex-grow-1">
                    <h6 className="mb-1">{item.name}</h6>
                    <p className="mb-1 small">Quantity: {item.quantity}</p>
                    <p className="mb-0 small">Price: ₹{(item.price * item.quantity).toFixed(2)}</p>
                  </div>
                </div>
              ))}

              <div className="text-center my-4">
                <h5 className="fw-bold">Total: ₹{totalPrice.toFixed(2)}</h5>
              </div>

              <Form.Group className="mb-3">
                <Form.Label>Name</Form.Label>
                <Form.Control
                  type="text"
                  placeholder="Enter your name"
                  value={name}
                  onChange={(e) => setName(e.target.value)}
                  required
                />
                <Form.Control.Feedback type="invalid">
                  Please provide your name.
                </Form.Control.Feedback>
              </Form.Group>

              <Form.Group className="mb-3">
                <Form.Label>Email</Form.Label>
                <Form.Control
                  type="email"
                  placeholder="Enter your email"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  required
                />
                <Form.Control.Feedback type="invalid">
                  Please provide a valid email address.
                </Form.Control.Feedback>
              </Form.Group>
            </div>
          </Modal.Body>
          <Modal.Footer>
            <Button variant="secondary" onClick={handleClose} disabled={isSubmitting}>
              Close
            </Button>
            <Button variant="success" type="submit" disabled={isSubmitting}>
              {isSubmitting ? (
                <>
                  <span className="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true"></span>
                  Processing...
                </>
              ) : (
                `Pay ₹${totalPrice.toFixed(2)}`
              )}
            </Button>
          </Modal.Footer>
        </Form>
      </Modal>

      <ToastContainer position="top-end" className="p-3" style={{ zIndex: 1070 }}>
        <Toast
          show={showToast}
          onClose={() => setShowToast(false)}
          delay={5000}
          autohide
          bg={toastVariant}
        >
          <Toast.Header closeButton>
            <strong className="me-auto">Order Status</strong>
          </Toast.Header>
          <Toast.Body className={toastVariant === 'success' ? 'text-white' : ''}>
            {toastMessage}
          </Toast.Body>
        </Toast>
      </ToastContainer>
    </>
  );
};

export default CheckoutPopup;
