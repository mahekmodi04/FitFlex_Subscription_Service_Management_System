import { useEffect, useState } from "react";
import QRCode from "qrcode";
import { CreditCard, Smartphone, Wallet } from "lucide-react";
import { PaymentMethod } from "@/types/enums";
import { formatCardNumber, formatExpiry, isExpiryValid, isUpiIdValid, luhnCheck } from "@/lib/cardValidation";
import { formatCurrency } from "@/lib/format";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Checkbox } from "@/components/ui/checkbox";

const METHODS = [
  { value: PaymentMethod.CARD, label: "Card", icon: CreditCard },
  { value: PaymentMethod.UPI, label: "UPI", icon: Smartphone },
  { value: PaymentMethod.WALLET, label: "Wallet", icon: Wallet },
];

// Card/UPI details never leave the browser — the backend's simulated gateway only accepts a
// PaymentMethod enum value, so this is purely a realistic-feeling front-end flow.
export function PaymentSection({
  paymentMethod,
  onPaymentMethodChange,
  cardDetails,
  onCardDetailsChange,
  upiId,
  onUpiIdChange,
  walletBalance,
  useWallet,
  onUseWalletChange,
  amountDue,
  fieldErrors = {},
}) {
  const [qrDataUrl, setQrDataUrl] = useState("");

  useEffect(() => {
    if (paymentMethod !== PaymentMethod.UPI) return;
    const payload = `upi://pay?pa=fitflex@upi&pn=FitFlex&am=${amountDue.toFixed(2)}&cu=INR&tn=FitFlex%20membership`;
    QRCode.toDataURL(payload, { width: 180, margin: 1, color: { dark: "#0f172a", light: "#ffffff" } })
      .then(setQrDataUrl)
      .catch(() => setQrDataUrl(""));
  }, [paymentMethod, amountDue]);

  const walletCoversAll = useWallet && walletBalance >= amountDue;

  return (
    <div className="space-y-4">
      <div className="space-y-1.5">
        <Label>Payment method</Label>
        <div className="grid grid-cols-3 gap-2">
          {METHODS.map((m) => (
            <button
              type="button"
              key={m.value}
              onClick={() => onPaymentMethodChange(m.value)}
              className={`flex flex-col items-center gap-1 rounded-lg border px-3 py-2.5 text-sm font-medium transition-colors ${
                paymentMethod === m.value
                  ? "border-accent bg-accent-soft text-accent-foreground"
                  : "border-border text-muted-foreground hover:border-accent/50"
              }`}
            >
              <m.icon className="size-4" />
              {m.label}
            </button>
          ))}
        </div>
      </div>

      {!walletCoversAll && paymentMethod === PaymentMethod.CARD && (
        <div className="space-y-3 rounded-lg border border-border bg-white p-4">
          <div className="space-y-1.5">
            <Label htmlFor="cardNumber">Card number</Label>
            <Input
              id="cardNumber"
              inputMode="numeric"
              placeholder="4242 4242 4242 4242"
              value={cardDetails.number}
              onChange={(e) => onCardDetailsChange({ ...cardDetails, number: formatCardNumber(e.target.value) })}
            />
            {fieldErrors.cardNumber && <p className="text-sm text-danger">{fieldErrors.cardNumber}</p>}
          </div>
          <div className="grid grid-cols-2 gap-3">
            <div className="space-y-1.5">
              <Label htmlFor="cardExpiry">Expiry (MM/YY)</Label>
              <Input
                id="cardExpiry"
                placeholder="12/28"
                value={cardDetails.expiry}
                onChange={(e) => onCardDetailsChange({ ...cardDetails, expiry: formatExpiry(e.target.value) })}
              />
              {fieldErrors.cardExpiry && <p className="text-sm text-danger">{fieldErrors.cardExpiry}</p>}
            </div>
            <div className="space-y-1.5">
              <Label htmlFor="cardCvv">CVV</Label>
              <Input
                id="cardCvv"
                inputMode="numeric"
                placeholder="123"
                maxLength={4}
                value={cardDetails.cvv}
                onChange={(e) => onCardDetailsChange({ ...cardDetails, cvv: e.target.value.replace(/\D/g, "").slice(0, 4) })}
              />
              {fieldErrors.cardCvv && <p className="text-sm text-danger">{fieldErrors.cardCvv}</p>}
            </div>
          </div>
        </div>
      )}

      {!walletCoversAll && paymentMethod === PaymentMethod.UPI && (
        <div className="space-y-3 rounded-lg border border-border bg-white p-4">
          <div className="space-y-1.5">
            <Label htmlFor="upiId">UPI ID</Label>
            <Input
              id="upiId"
              placeholder="yourname@bank"
              value={upiId}
              onChange={(e) => onUpiIdChange(e.target.value)}
            />
            {fieldErrors.upiId && <p className="text-sm text-danger">{fieldErrors.upiId}</p>}
          </div>
          {qrDataUrl && (
            <div className="flex flex-col items-center gap-2 pt-2">
              <img src={qrDataUrl} alt="UPI payment QR code" className="rounded-lg border border-border" />
              <p className="text-xs text-muted-foreground">Scan with any UPI app to pay {formatCurrency(amountDue)}</p>
            </div>
          )}
        </div>
      )}

      {!walletCoversAll && paymentMethod === PaymentMethod.WALLET && (
        <div className="rounded-lg border border-border bg-white p-4 text-sm text-muted-foreground">
          You&apos;ll pay {formatCurrency(amountDue)} directly through the simulated wallet gateway.
        </div>
      )}

      <div className="flex items-start gap-3 rounded-lg border border-border bg-accent-soft/40 p-4">
        <Checkbox
          id="useWallet"
          checked={useWallet}
          onCheckedChange={onUseWalletChange}
          disabled={walletBalance <= 0}
        />
        <div>
          <Label htmlFor="useWallet" className="font-medium text-ink">
            Use my FitFlex wallet balance
          </Label>
          <p className="text-xs text-muted-foreground">
            {walletBalance > 0
              ? `${formatCurrency(walletBalance)} available — earned ₹25 back on every subscription payment.`
              : "No wallet balance yet — you'll earn ₹25 back after this payment."}
          </p>
        </div>
      </div>
    </div>
  );
}

export function validatePaymentFields(paymentMethod, cardDetails, upiId) {
  const errors = {};
  if (paymentMethod === PaymentMethod.CARD) {
    if (!luhnCheck(cardDetails.number)) errors.cardNumber = "Enter a valid card number.";
    if (!isExpiryValid(cardDetails.expiry)) errors.cardExpiry = "Enter a valid, unexpired MM/YY.";
    if (!/^\d{3,4}$/.test(cardDetails.cvv)) errors.cardCvv = "Enter a valid CVV.";
  } else if (paymentMethod === PaymentMethod.UPI) {
    if (!isUpiIdValid(upiId)) errors.upiId = "Enter a valid UPI ID, e.g. yourname@bank.";
  }
  return errors;
}
