const axios = require('axios');
const { v4: uuidv4 } = require('uuid');
// const faker = require('faker');

// Create a reusable Axios instance
const instance = axios.create();

// Function to execute requests for a single user
const executeRequests = async (baseURL, iterationsPerUser, endpoints, userId) => {
  // Execute requests for each iteration
  for (let j = 0; j < iterationsPerUser; j++) {
    for (const endpoint of endpoints) {
      try {
        const response = await instance.get(`${baseURL}/${endpoint}`);
        console.log(`Response for user ${userId} to ${endpoint}: ${response.status}`);
      } catch (error) {
        console.error(`Error sending request for user ${userId} to ${endpoint}:`, error.message);
      }
      // Function to gradually increase the number of users
     await new Promise(resolve => setTimeout(resolve, 10000));
    }
  }
};

// Function to simulate user traffic
const simulateTraffic = async (baseURL, totalUsers, duration, iterationsPerUser, endpoints) => {
  const startTime = Date.now();
  let currentUserCount = 0;

  // Function to gradually increase the number of users
  const increaseUserCount = async () => {
    while (currentUserCount < totalUsers && Date.now() - startTime < duration) {
      const userId = uuidv4();
      executeRequests(baseURL, iterationsPerUser, endpoints, userId); // Start user requests asynchronously
      currentUserCount++;
      console.log(`Started requests for user number: ${currentUserCount}`);

      // Wait for a random period between 1 to 60 seconds before adding the next user
      const waitTime = Math.random() * 20000; // Random time between 1 to 60 seconds
      await new Promise(resolve => setTimeout(resolve, waitTime));
    }
  };

  // Start the simulation
  await increaseUserCount();
  console.log('Traffic injection process started.');
};

// Set up parameters
// const baseURL = 'http://ec2-loadbalancer-880256090.us-east-2.elb.amazonaws.com:8080';
const baseURL = 'http://localhost:8080';
const totalUsers = 1; // Total number of users to simulate
const duration = 1 * 60 * 1000; // Duration in milliseconds (15 minutes)
const iterationsPerUser = 5; // Number of iterations per user
const endpoints = ['call-c'];

// Run the simulation
simulateTraffic(baseURL, totalUsers, duration, iterationsPerUser, endpoints)
  .then(() => console.log('Traffic injection process completed.'))
  .catch(error => console.error('Error:', error));

 

/**

  To execute this code, follow these steps:

1. **Install Node.js**  
  Make sure Node.js is installed on your system. You can download it from [nodejs.org](https://nodejs.org/).

2. **Install Required Packages**  
  Open a terminal in the directory containing your script and run:
  ```
  npm install uuid
  ```
  If you uncomment the `axios` and `faker` lines, also run:
  ```
  npm install axios faker
  ```

3. **Uncomment Required Code**  
  Uncomment the `axios` import and related lines if you want to use Axios for HTTP requests.

4. **Run the Script**  
  In your terminal, execute:
  ```
  node traffic_injection_script_concurrent_users.js
  ```

This will start the traffic injection simulation as defined in your script.

*/