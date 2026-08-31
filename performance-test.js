import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter, Trend, Rate } from 'k6/metrics';

// Custom metrics
const loginCounter = new Counter('login_total');
const apiDataTrend = new Trend('api_data_latency');
const orchestrateTrend = new Trend('orchestrate_latency');
const errorRate = new Rate('errors');
const authErrorRate = new Rate('auth_errors');

// Configuration
const BASE_URL = 'http://localhost:8080';
const SERVICE_A_URL = 'http://localhost:8081';
const SERVICE_B_URL = 'http://localhost:8082';
const SERVICE_C_URL = 'http://localhost:8083';

export const options = {
  stages: [
    { duration: '30s', target: 5 },
    { duration: '1m', target: 15 },
    { duration: '2m', target: 30 },
    { duration: '3m', target: 30 },
    { duration: '30s', target: 50 },
    { duration: '1m', target: 0 },
  ],
  thresholds: {
    'http_req_duration': ['p(95)<500', 'p(99)<1000'],  // 95% zahteva < 500ms, 99% < 1000ms
    'http_req_failed': ['rate<0.1'],                   // Manje od 10% neuspešnih zahteva
    'errors': ['rate<0.1'],
  },
};

// Helper function za login i dobijanje JWT tokena
function getJWTToken(username, password) {
  const payload = {
    username: username,
    password: password,
    clientType: username === 'user1' ? 'USER' : 'SERVICE',
  };

  const response = http.post(`${BASE_URL}/auth/login`, JSON.stringify(payload), {
    headers: { 'Content-Type': 'application/json' },
  });

  check(response, {
    'login status is 200': (r) => r.status === 200,
  });

  if (response.status === 200) {
    loginCounter.add(1);
    const data = JSON.parse(response.body);
    return data.token;
  } else {
    authErrorRate.add(1);
    return null;
  }
}

// Test scenario 1: Autentifikacija korisnika
export function testUserAuthentication() {
  const token = getJWTToken('user1', 'password123');

  if (token) {
    check(token, {
      'token is not empty': (t) => t && t.length > 0,
    });
  }

  sleep(1);
}

// Test scenario 2: Pristup zaštićenom resursu SA JWT-om
export function testProtectedEndpointWithToken() {
  const token = getJWTToken('user1', 'password123');

  if (token) {
    const response = http.get(`${SERVICE_A_URL}/api/data`, {
      headers: {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json',
      },
    });

    apiDataTrend.add(response.timings.duration);

    check(response, {
      'GET /api/data status is 200': (r) => r.status === 200,
      'response time < 100ms': (r) => r.timings.duration < 100,
    });

    if (response.status !== 200) {
      errorRate.add(1);
    }
  }

  sleep(1);
}

// Test scenario 3: Pristup zaštićenom resursu BEZ JWT-a
export function testProtectedEndpointWithoutToken() {
  const response = http.get(`${SERVICE_A_URL}/api/data`, {
    headers: { 'Content-Type': 'application/json' },
  });

  check(response, {
    'GET /api/data without token returns 401': (r) => r.status === 401,
  });

  if (response.status !== 401) {
    errorRate.add(1);
  }

  sleep(1);
}

// Test scenario 4: Javna putanja BEZ JWT-a
export function testPublicEndpoint() {
  const response = http.get(`${SERVICE_A_URL}/api/public/health`, {
    headers: { 'Content-Type': 'application/json' },
  });

  check(response, {
    'GET /api/public/health status is 200': (r) => r.status === 200,
    'response time < 50ms': (r) => r.timings.duration < 50,
  });

  if (response.status !== 200) {
    errorRate.add(1);
  }

  sleep(1);
}

// Test scenario 5: Service-to-service komunikacija (Service B poziva Service A)
export function testServiceToServiceAuth() {
  const userToken = getJWTToken('user1', 'password123');

  if (userToken) {
    const response = http.get(`${SERVICE_B_URL}/api/orchestrate`, {
      headers: {
        'Authorization': `Bearer ${userToken}`,
        'Content-Type': 'application/json',
      },
    });

    orchestrateTrend.add(response.timings.duration);

    check(response, {
      'GET /api/orchestrate status is 200': (r) => r.status === 200,
      'response time < 200ms': (r) => r.timings.duration < 200,
    });

    if (response.status !== 200) {
      errorRate.add(1);
    }
  }

  sleep(1);
}

// Test scenario 6: Kompletan lanac (Service C → Service B → Service A)
export function testFullChain() {
  const userToken = getJWTToken('user1', 'password123');

  if (userToken) {
    const response = http.get(`${SERVICE_C_URL}/api/orchestrate`, {
      headers: {
        'Authorization': `Bearer ${userToken}`,
        'Content-Type': 'application/json',
      },
    });

    orchestrateTrend.add(response.timings.duration);

    check(response, {
      'GET /api/orchestrate (Service C) status is 200': (r) => r.status === 200,
      'response time < 300ms': (r) => r.timings.duration < 300,
    });

    if (response.status !== 200) {
      errorRate.add(1);
    }
  }

  sleep(1);
}

// Main test execution - kombinacija svih scenarija
export default function() {
  const scenario = Math.floor(Math.random() * 6);

  switch(scenario) {
    case 0:
      testUserAuthentication();
      break;
    case 1:
      testProtectedEndpointWithToken();
      break;
    case 2:
      testProtectedEndpointWithoutToken();
      break;
    case 3:
      testPublicEndpoint();
      break;
    case 4:
      testServiceToServiceAuth();
      break;
    case 5:
      testFullChain();
      break;
  }
}

export function handleSummary(data) {
  const totalRequests = data.metrics.http_reqs.values.count;
  const failedRate = data.metrics.http_req_failed.values.rate;
  const totalFailed = Math.round(totalRequests * failedRate);
  const p95 = data.metrics.http_req_duration.values['p(95)'];
  const p99 = data.metrics.http_req_duration.values['p(99)'];

  console.log('Test Summary:');
  console.log('=============');
  console.log(`Total Requests: ${totalRequests}`);
  console.log(`Total Errors: ${totalFailed}`);
  console.log(`Error Rate: ${(failedRate * 100).toFixed(2)}%`);
  console.log(`p95 Response Time: ${p95.toFixed(2)}ms`);
  console.log(`p99 Response Time: ${p99.toFixed(2)}ms`);

  return {}; // sprečava k6 da dodatno ispisuje default summary
}