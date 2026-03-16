const express = require("express");
const { Pool } = require("pg");

const app = express();
app.use(express.json());

const pool = new Pool({
  host: "db",
  user: "postgres",
  password: "postgres",
  database: "appdb",
  port: 5432
});

app.get("/", (req, res) => {
  res.send("Containerized Web App Running");
});

app.get("/users", async (req, res) => {
  const result = await pool.query("SELECT * FROM users");
  res.json(result.rows);
});

app.post("/users", async (req, res) => {
  const { name } = req.body;
  const result = await pool.query(
    "INSERT INTO users(name) VALUES($1) RETURNING *",
    [name]
  );
  res.json(result.rows[0]);
});

app.listen(3000, () => {
  console.log("Server running on port 3000");
});