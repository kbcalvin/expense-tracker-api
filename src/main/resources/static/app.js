const authView = document.querySelector("#auth-view");
const appView = document.querySelector("#app-view");
const authForm = document.querySelector("#auth-form");
const authMessage = document.querySelector("#auth-message");
const expenseMessage = document.querySelector("#expense-message");
const tokenKey = "ledgerToken";
let authMode = "login";
let expenses = [];

const money = new Intl.NumberFormat(undefined, {
  style: "currency",
  currency: "USD"
});

function showMessage(element, message = "") {
  element.textContent = message;
}

document.querySelector("#today-label").textContent = new Intl.DateTimeFormat(undefined, {
  weekday: "long",
  month: "long",
  day: "numeric",
  year: "numeric"
}).format(new Date()).toUpperCase();

async function api(path, options = {}) {
  const headers = new Headers(options.headers || {});
  if (options.body) headers.set("Content-Type", "application/json");
  const token = sessionStorage.getItem(tokenKey);
  if (token) headers.set("Authorization", `Bearer ${token}`);

  const response = await fetch(path, { ...options, headers });
  if (!response.ok) {
    let message = `Request failed (${response.status})`;
    try {
      const body = await response.json();
      message = body.detail || body.message || body.error || message;
    } catch {
      // Keep the status-based message when the server doesn't return JSON.
    }
    throw new Error(message);
  }

  if (response.status === 204) return null;
  return response.json();
}

function setAuthMode(mode) {
  authMode = mode;
  const registering = mode === "register";
  document.querySelector("#auth-title").textContent = registering ? "Start fresh." : "Welcome back.";
  document.querySelector("#auth-subtitle").textContent = registering
    ? "Create an account to start tracking your expenses."
    : "Sign in to pick up where you left off.";
  document.querySelector("#auth-submit").innerHTML = registering
    ? 'Create account <span aria-hidden="true">→</span>'
    : 'Sign in <span aria-hidden="true">→</span>';
  document.querySelector("#auth-switch-copy").textContent = registering ? "Already have an account?" : "New to Ledger?";
  document.querySelector("#auth-switch").textContent = registering ? "Sign in" : "Create an account";
  authForm.elements.password.autocomplete = registering ? "new-password" : "current-password";
  showMessage(authMessage);
}

function showAuth() {
  sessionStorage.removeItem(tokenKey);
  authView.hidden = false;
  appView.hidden = true;
  authForm.reset();
  setAuthMode("login");
}

function formatDate(value) {
  return new Date(`${value}T12:00:00`).toLocaleDateString(undefined, {
    month: "short",
    day: "numeric",
    year: "numeric"
  });
}

function makeExpenseRow(expense) {
  const row = document.createElement("div");
  row.className = "expense-row";

  const description = document.createElement("div");
  description.className = "expense-description";
  const symbol = document.createElement("span");
  symbol.className = "expense-symbol";
  symbol.setAttribute("aria-hidden", "true");
  symbol.textContent = (expense.category || "E").slice(0, 1).toUpperCase();
  const title = document.createElement("span");
  title.className = "expense-title";
  title.textContent = expense.title;
  description.append(symbol, title);

  const category = document.createElement("span");
  category.className = "category-pill";
  category.textContent = expense.category || "Uncategorized";

  const date = document.createElement("span");
  date.className = "expense-date";
  date.textContent = formatDate(expense.date);

  const amount = document.createElement("span");
  amount.className = "expense-amount";
  amount.textContent = `−${money.format(Number(expense.amount))}`;

  row.append(description, category, date, amount);
  return row;
}

function renderCategories(totals) {
  const target = document.querySelector("#category-list");
  target.replaceChildren();
  const items = Object.entries(totals).sort((a, b) => b[1] - a[1]);
  if (!items.length) {
    const empty = document.createElement("p");
    empty.className = "category-empty";
    empty.textContent = "Your spending breakdown will show here.";
    target.append(empty);
    return;
  }

  const max = items[0][1] || 1;
  for (const [name, total] of items) {
    const item = document.createElement("div");
    item.className = "category-item";
    const meta = document.createElement("div");
    meta.className = "category-meta";
    const label = document.createElement("span");
    label.textContent = name;
    const value = document.createElement("strong");
    value.textContent = money.format(total);
    meta.append(label, value);
    const track = document.createElement("div");
    track.className = "category-track";
    const bar = document.createElement("div");
    bar.className = "category-bar";
    bar.style.width = `${Math.max(4, (total / max) * 100)}%`;
    track.append(bar);
    item.append(meta, track);
    target.append(item);
  }
}

function renderExpenses(summary) {
  const list = document.querySelector("#expense-list");
  list.replaceChildren();
  const ordered = [...expenses].sort((a, b) => b.date.localeCompare(a.date) || b.id - a.id);
  for (const expense of ordered) list.append(makeExpenseRow(expense));

  const total = expenses.reduce((sum, item) => sum + Number(item.amount), 0);
  const now = new Date();
  const monthTotal = expenses
    .filter(item => {
      const date = new Date(`${item.date}T12:00:00`);
      return date.getFullYear() === now.getFullYear() && date.getMonth() === now.getMonth();
    })
    .reduce((sum, item) => sum + Number(item.amount), 0);

  document.querySelector("#total-spending").textContent = money.format(total);
  document.querySelector("#month-spending").textContent = money.format(monthTotal);
  document.querySelector("#expense-count").textContent = String(expenses.length);
  document.querySelector("#record-count").textContent = `${expenses.length} ${expenses.length === 1 ? "record" : "records"}`;
  document.querySelector("#category-count").textContent = `${Object.keys(summary).length} ${Object.keys(summary).length === 1 ? "category" : "categories"}`;
  document.querySelector("#empty-state").hidden = expenses.length > 0;
  renderCategories(summary);
}

async function loadDashboard() {
  const [loadedExpenses, summary] = await Promise.all([
    api("/api/expenses"),
    api("/api/expenses/summary")
  ]);
  expenses = loadedExpenses;
  renderExpenses(summary);
  authView.hidden = true;
  appView.hidden = false;

  const email = sessionStorage.getItem("ledgerEmail") || "Account";
  document.querySelector("#profile-email").textContent = email;
  document.querySelector("#profile-avatar").textContent = email.slice(0, 1).toUpperCase();
  document.querySelector("#expense-form").elements.date.value = new Date().toISOString().slice(0, 10);
}

document.querySelector("#auth-switch").addEventListener("click", () => {
  setAuthMode(authMode === "login" ? "register" : "login");
});

authForm.addEventListener("submit", async event => {
  event.preventDefault();
  const button = document.querySelector("#auth-submit");
  button.disabled = true;
  showMessage(authMessage);
  try {
    const body = Object.fromEntries(new FormData(authForm));
    const result = await api(`/api/auth/${authMode}`, {
      method: "POST",
      body: JSON.stringify(body)
    });
    sessionStorage.setItem(tokenKey, result.token);
    sessionStorage.setItem("ledgerEmail", result.email || body.email);
    await loadDashboard();
  } catch (error) {
    showMessage(authMessage, error.message);
  } finally {
    button.disabled = false;
  }
});

document.querySelector("#expense-form").addEventListener("submit", async event => {
  event.preventDefault();
  const form = event.currentTarget;
  const button = document.querySelector("#expense-submit");
  button.disabled = true;
  showMessage(expenseMessage);
  try {
    const body = Object.fromEntries(new FormData(form));
    body.amount = Number(body.amount);
    await api("/api/expenses", { method: "POST", body: JSON.stringify(body) });
    form.reset();
    form.elements.date.value = new Date().toISOString().slice(0, 10);
    showMessage(expenseMessage, "Expense saved.");
    await loadDashboard();
    window.setTimeout(() => showMessage(expenseMessage), 2200);
  } catch (error) {
    showMessage(expenseMessage, error.message);
  } finally {
    button.disabled = false;
  }
});

document.querySelector("#sign-out").addEventListener("click", showAuth);
document.querySelector("#jump-to-form").addEventListener("click", () => {
  document.querySelector("#add-expense").scrollIntoView({ behavior: "smooth", block: "center" });
  document.querySelector("#expense-form").elements.title.focus({ preventScroll: true });
});

if (sessionStorage.getItem(tokenKey)) {
  loadDashboard().catch(showAuth);
} else {
  authView.hidden = false;
  appView.hidden = true;
  setAuthMode("login");
}
