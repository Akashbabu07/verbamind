import { useEffect, useState } from "react";
import { useOrg } from "../../context/OrgContext";
import {
  listPlans,
  getSubscription,
  upgradeSubscription,
  cancelSubscription,
  createPaymentOrder,
  verifyPayment,
  paymentHistory,
} from "../../api/resources";

function loadRazorpayScript() {
  return new Promise((resolve) => {
    if (window.Razorpay) return resolve(true);
    const script = document.createElement("script");
    script.src = "https://checkout.razorpay.com/v1/checkout.js";
    script.onload = () => resolve(true);
    script.onerror = () => resolve(false);
    document.body.appendChild(script);
  });
}

export default function Billing() {
  const { activeOrgId } = useOrg();
  const [plans, setPlans] = useState([]);
  const [subscription, setSubscription] = useState(null);
  const [history, setHistory] = useState([]);
  const [busyPlanId, setBusyPlanId] = useState(null);

  useEffect(() => {
    listPlans().then(setPlans).catch(() => {});
  }, []);

  useEffect(() => {
    if (!activeOrgId) return;
    getSubscription(activeOrgId).then(setSubscription).catch(() => {});
    paymentHistory(activeOrgId)
      .then((data) => setHistory(data.items || data))
      .catch(() => {});
  }, [activeOrgId]);

  const onUpgrade = async (plan) => {
    setBusyPlanId(plan.id);
    try {
      // Every payment plan requires a real transaction — free/downgrade tiers
      // just call upgrade directly, paid tiers go through Razorpay checkout.
      if (!plan.priceInPaise || plan.priceInPaise === 0) {
        await upgradeSubscription(activeOrgId, plan.id);
        const updated = await getSubscription(activeOrgId);
        setSubscription(updated);
        return;
      }

      const ready = await loadRazorpayScript();
      if (!ready) {
        alert("Couldn't load the payment gateway. Check your connection and try again.");
        return;
      }

      // Only an order id + the account's public key ever reach the browser.
      // Verification of the payment signature happens server-side.
      const order = await createPaymentOrder(activeOrgId, { planId: plan.id });

      const checkout = new window.Razorpay({
        key: order.razorpayKeyId,
        amount: order.amount,
        currency: order.currency,
        order_id: order.orderId,
        name: "Verbamind",
        description: `${plan.name} plan`,
        handler: async (response) => {
          await verifyPayment(activeOrgId, {
            razorpayOrderId: response.razorpay_order_id,
            razorpayPaymentId: response.razorpay_payment_id,
            razorpaySignature: response.razorpay_signature,
          });
          const updated = await getSubscription(activeOrgId);
          setSubscription(updated);
          const refreshedHistory = await paymentHistory(activeOrgId);
          setHistory(refreshedHistory.items || refreshedHistory);
        },
        theme: { color: "#1a1a1a" },
      });

      checkout.open();
    } finally {
      setBusyPlanId(null);
    }
  };

  const onCancel = async () => {
    if (!confirm("Cancel your subscription?")) return;
    await cancelSubscription(activeOrgId);
    const updated = await getSubscription(activeOrgId);
    setSubscription(updated);
  };

  return (
    <div>
      <h1>Billing</h1>

      <section className="section">
        <h2>Current plan</h2>
        <p>
          {subscription?.planName || "Free"} — <span className="muted">{subscription?.status}</span>
        </p>
        {subscription?.planName && subscription.planName !== "Free" && (
          <button className="link-btn danger" onClick={onCancel}>
            Cancel subscription
          </button>
        )}
      </section>

      <section className="section">
        <h2>Plans</h2>
        <div className="card-grid">
          {plans.map((plan) => (
            <div className="card" key={plan.id}>
              <h3>{plan.name}</h3>
              <p className="stat">
                {plan.priceInPaise ? `₹${plan.priceInPaise / 100}/mo` : "Free"}
              </p>
              <ul className="plan-features">
                {(plan.features || []).map((f) => (
                  <li key={f}>{f}</li>
                ))}
              </ul>
              <button
                className="btn primary"
                disabled={busyPlanId === plan.id || subscription?.planId === plan.id}
                onClick={() => onUpgrade(plan)}
              >
                {subscription?.planId === plan.id
                  ? "Current plan"
                  : busyPlanId === plan.id
                  ? "Processing…"
                  : "Choose plan"}
              </button>
            </div>
          ))}
        </div>
      </section>

      <section className="section">
        <h2>Payment history</h2>
        {history.length === 0 ? (
          <p className="muted">No payments yet.</p>
        ) : (
          <table className="data-table">
            <thead>
              <tr>
                <th>Date</th>
                <th>Amount</th>
                <th>Status</th>
              </tr>
            </thead>
            <tbody>
              {history.map((p) => (
                <tr key={p.id}>
                  <td>{new Date(p.createdAt).toLocaleDateString()}</td>
                  <td>₹{p.amount / 100}</td>
                  <td>{p.status}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </section>
    </div>
  );
}
