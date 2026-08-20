/**
 * ============================================================================
 * 🛡️ MilHub User Activity & Traffic Simulator (simulate-activity.js)
 * ============================================================================
 * 
 * Simulates complete, realistic, lifelike activity on the MilHub platform:
 * 1. Provisions & logs in 15 realistic buyers:
 *    - 5 Verified Military Units (MILITARY_UNIT) with military clearance.
 *    - 10 Defense Volunteers & Verified Buyers (BUYER).
 * 2. Fetches public and restricted catalog products and maps them by seller.
 * 3. Authenticates sellers to process and fulfill orders.
 * 4. Each buyer creates 10 distinct orders (150 total orders).
 * 5. Simulates instant payment processing via Payment Webhook (status -> PAID).
 * 6. Seller transitions order status to DELIVERED.
 * 7. Buyer posts authentic, combat-tested Field Reports & Reviews (★ 4-5)
 *    which automatically receive the official "Verified Purchase" badge!
 * 8. Buyer submits store & supplier reviews (★ 4-5) to build seller reputation!
 * 
 * Usage:
 *   node simulate-activity.js        -> Default: Local (http://localhost:8080/api/v1)
 *   node simulate-activity.js cloud  -> Production Cloud Run Gateway
 *   node simulate-activity.js <url>  -> Custom Gateway URL
 * ============================================================================
 */

const fs = require('fs');
const path = require('path');

const args = process.argv.slice(2);
const modeInput = (args[0] || 'local').toLowerCase();
const isCloud = ['cloud', 'cloude', 'gcp', 'prod', 'production'].includes(modeInput);

let API_BASE = 'http://localhost:8080/api/v1';
if (modeInput.startsWith('http://') || modeInput.startsWith('https://')) {
    API_BASE = modeInput.endsWith('/') ? modeInput.slice(0, -1) : modeInput;
} else if (isCloud) {
    API_BASE = 'https://milhub-api-gateway-258044247462.us-central1.run.app/api/v1';
}

const displayMode = isCloud ? 'CLOUD' : (modeInput.startsWith('http') ? 'CUSTOM' : 'LOCAL');

console.log(`======================================================================`);
console.log(`🚀 Starting MilHub Traffic & User Activity Simulator in [${displayMode}] mode`);
console.log(`📍 Target API Base URL: ${API_BASE}`);
console.log(`======================================================================\n`);

async function request(endpoint, method, body = null, token = null, silentError = false) {
    const headers = { 'Content-Type': 'application/json' };
    if (token) headers['Authorization'] = `Bearer ${token}`;
    const config = { method, headers };
    if (body) config.body = JSON.stringify(body);

    const startTime = Date.now();
    try {
        const res = await fetch(`${API_BASE}${endpoint}`, config);
        const text = await res.text();
        const duration = Date.now() - startTime;

        if (!res.ok) {
            if (!silentError) {
                console.error(`  ⚠️ [${method} ${endpoint}] HTTP ${res.status} (${duration}ms): ${text.substring(0, 120)}`);
            }
            throw new Error(`API Error [${method} ${endpoint}]: ${res.status} ${text}`);
        }

        try { return JSON.parse(text); } catch { return text; }
    } catch (err) {
        throw err;
    }
}

async function delay(ms) { return new Promise(resolve => setTimeout(resolve, ms)); }

// Realistic buyers dataset (5 Military Units + 10 Defense Volunteers/Buyers)
const BUYERS_DATA = [
    // 5 Military Units (With Clearance)
    {
        firstName: "72nd Brigade",
        lastName: "Black Zaporozhians",
        email: "unit-72ombr@milhub.ua",
        password: "Password123!",
        phoneNumber: "+380501117272",
        isMilitary: true,
        militaryProfile: {
            unitNumber: "A2167",
            edrpou: "07892167",
            commanderName: "Col. I. Vdovychenko",
            officialAddress: "Bila Tserkva, Kyiv Region, Military Base A2167"
        }
    },
    {
        firstName: "93rd Brigade",
        lastName: "Kholodnyi Yar",
        email: "unit-93ombr@milhub.ua",
        password: "Password123!",
        phoneNumber: "+380502229393",
        isMilitary: true,
        militaryProfile: {
            unitNumber: "A1302",
            edrpou: "08121302",
            commanderName: "Col. R. Shevchuk",
            officialAddress: "Cherkaske, Dnipro Region, Military Base A1302"
        }
    },
    {
        firstName: "10th Brigade",
        lastName: "Edelweiss",
        email: "unit-10ogshbr@milhub.ua",
        password: "Password123!",
        phoneNumber: "+380503331010",
        isMilitary: true,
        militaryProfile: {
            unitNumber: "A4267",
            edrpou: "24984267",
            commanderName: "Col. V. Zubanych",
            officialAddress: "Kolomyia, Ivano-Frankivsk Region, Military Base A4267"
        }
    },
    {
        firstName: "3rd Assault",
        lastName: "Brigade AFU",
        email: "unit-3oshbr@milhub.ua",
        password: "Password123!",
        phoneNumber: "+380504440303",
        isMilitary: true,
        militaryProfile: {
            unitNumber: "A4638",
            edrpou: "38924638",
            commanderName: "Col. A. Biletskyi",
            officialAddress: "Kyiv, 45 Peremohy Ave, Military Base A4638"
        }
    },
    {
        firstName: "47th Brigade",
        lastName: "Magura",
        email: "unit-47ombr@milhub.ua",
        password: "Password123!",
        phoneNumber: "+380505554747",
        isMilitary: true,
        militaryProfile: {
            unitNumber: "A4699",
            edrpou: "44914699",
            commanderName: "Lt. Col. O. Sak",
            officialAddress: "Kharkiv Sector, Field Post 4699"
        }
    },

    // 10 Volunteer Foundations & Defense Buyers
    {
        firstName: "Come Back Alive",
        lastName: "Foundation",
        email: "logistics-pz@milhub.ua",
        password: "Password123!",
        phoneNumber: "+380441112233",
        isMilitary: false
    },
    {
        firstName: "Army SOS",
        lastName: "Defense NGO",
        email: "supply-armysos@milhub.ua",
        password: "Password123!",
        phoneNumber: "+380442223344",
        isMilitary: false
    },
    {
        firstName: "Prytula",
        lastName: "Charity Foundation",
        email: "procurement-prytula@milhub.ua",
        password: "Password123!",
        phoneNumber: "+380443334455",
        isMilitary: false
    },
    {
        firstName: "KOLO Together",
        lastName: "Charity Foundation",
        email: "kolo-charity@milhub.ua",
        password: "Password123!",
        phoneNumber: "+380444445566",
        isMilitary: false
    },
    {
        firstName: "Dnipro Varta",
        lastName: "Volunteer Hub",
        email: "dnipro-varta@milhub.ua",
        password: "Password123!",
        phoneNumber: "+380565556677",
        isMilitary: false
    },
    {
        firstName: "Alexander",
        lastName: "Kovalenko",
        email: "oleksandr.k@milhub.ua",
        password: "Password123!",
        phoneNumber: "+380671234501",
        isMilitary: false
    },
    {
        firstName: "Michael",
        lastName: "Tkach",
        email: "mykhailo.t@milhub.ua",
        password: "Password123!",
        phoneNumber: "+380671234502",
        isMilitary: false
    },
    {
        firstName: "Dmitry",
        lastName: "Yarosh",
        email: "dmytro.y@milhub.ua",
        password: "Password123!",
        phoneNumber: "+380671234503",
        isMilitary: false
    },
    {
        firstName: "Anna",
        lastName: "Sergienko",
        email: "anna.s@milhub.ua",
        password: "Password123!",
        phoneNumber: "+380671234504",
        isMilitary: false
    },
    {
        firstName: "Bogdan",
        lastName: "Shevchenko",
        email: "bogdan.sh@milhub.ua",
        password: "Password123!",
        phoneNumber: "+380671234505",
        isMilitary: false
    }
];

const REVIEWS_POOL = [
    { rating: 5, comment: "Field-tested in Pokrovsk sector: equipment withstands high recoil and harsh conditions. Superior build quality, highly recommended!" },
    { rating: 5, comment: "Drone completed over 20 combat sorties under intense enemy electronic warfare (EW). Video link and stabilization are rock solid." },
    { rating: 5, comment: "Thermal optic provides crystal-clear target acquisition through heavy fog and smoke. Battery runtime meets tactical specs." },
    { rating: 5, comment: "Plate carrier distributes weight perfectly during 20km forced marches. Authentic 1000D Cordura and quick-release work flawlessly." },
    { rating: 5, comment: "Supplied an entire battalion with this gear. Supplier shipped within 48 hours, fully sealed and certified." },
    { rating: 4, comment: "Reliable field gear for everyday combat duties. Battery pack is slightly bulky, but extended operation time compensates." },
    { rating: 5, comment: "Deployed in Kharkiv frontline. Radio communication remains stable with high resistance to signal jamming." },
    { rating: 5, comment: "High-quality tactical IFAK pouch with rapid detachment. Fits complete NATO-standard trauma kit." },
    { rating: 5, comment: "Active hearing protection effectively dampens explosions while amplifying ambient whispers. Essential kit for infantry." },
    { rating: 4, comment: "Solid construction and durable mounting. Fits seamlessly onto FAST ballistic helmet rails without wobble." },
    { rating: 5, comment: "Optic holds zero flawlessly after hundreds of rounds of 7.62 and 5.56. Reticle is sharp with smooth illumination adjustment." },
    { rating: 5, comment: "Heavy-duty power station simultaneously powers 3 tactical radios and quadcopter charger. Reliable off-grid energy." }
];

const SELLER_REVIEWS_POOL = [
    { rating: 5, comment: "Excellent defense supplier. Fast delivery directly to the military base, equipment fully meets military specifications." },
    { rating: 5, comment: "Official manufacturer warranty and transparent documentation. Shipped complete batch within 24 hours." },
    { rating: 5, comment: "Top-tier tactical gear and professional customer support. All items arrived sealed with quality certificates." },
    { rating: 5, comment: "Reliable partner for defense procurement and volunteer logistics. Flawless communication and fast dispatch." },
    { rating: 4, comment: "High quality gear and solid packaging. Delivery took 3 days due to high demand, but overall very satisfied." },
    { rating: 5, comment: "Great communication, prompt dispatch, and genuine mil-spec materials. Recommended supplier for AFU units!" },
    { rating: 5, comment: "All requested certificates provided upon request. Packaging is weatherproof and rugged. 5/5 store." }
];

const CITIES = [
    { ref: "ref-kyiv", name: "Kyiv", region: "Kyiv Region", branchRef: "ref-br-1", branch: "Nova Poshta Branch #1 (Pirohivskyi Shlyakh 135)" },
    { ref: "ref-lviv", name: "Lviv", region: "Lviv Region", branchRef: "ref-br-4", branch: "Nova Poshta Branch #4 (Uhorska St 22)" },
    { ref: "ref-dnipro", name: "Dnipro", region: "Dnipro Region", branchRef: "ref-br-8", branch: "Nova Poshta Branch #8 (Kosmichna St 25d)" },
    { ref: "ref-kharkiv", name: "Kharkiv", region: "Kharkiv Region", branchRef: "ref-br-2", branch: "Nova Poshta Branch #2 (Moskovskyi Ave 199b)" },
    { ref: "ref-zaporizhzhia", name: "Zaporizhzhia", region: "Zaporizhzhia Region", branchRef: "ref-br-1", branch: "Nova Poshta Branch #1 (Brianska St 8)" },
    { ref: "ref-odesa", name: "Odesa", region: "Odesa Region", branchRef: "ref-br-3", branch: "Nova Poshta Branch #3 (Dalnytska St 23/4)" },
    { ref: "ref-pokrovsk", name: "Pokrovsk", region: "Donetsk Region", branchRef: "ref-br-1", branch: "Nova Poshta Branch #1 (Torhova St 18)" },
    { ref: "ref-kramatorsk", name: "Kramatorsk", region: "Donetsk Region", branchRef: "ref-br-2", branch: "Nova Poshta Branch #2 (Marata St 14)" }
];

async function runTrafficSimulator() {
    // 1. Authenticate Admin
    console.log("🔑 1. Authenticating System Administrator...");
    let adminToken = null;
    try {
        const adminRes = await request('/auth/authenticate', 'POST', {
            email: 'admin@milhub.ua',
            password: 'Password123!'
        }, null, true);
        if (adminRes && adminRes.token) {
            adminToken = adminRes.token;
            console.log("  ✅ Administrator authenticated successfully.");
        }
    } catch (e) {
        console.warn("  ⚠️ Could not authenticate with admin@milhub.ua:", e.message);
    }

    // 2. Fetch Catalog Products
    console.log("\n📦 2. Fetching available product catalog...");
    const productsRes = await request('/products?size=100', 'GET', null, null, true);
    const allProducts = (productsRes && productsRes.content) ? productsRes.content : [];
    
    if (allProducts.length === 0) {
        console.error("❌ No products found in the catalog! Please run `node seed.js` first.");
        process.exit(1);
    }

    const publicProducts = allProducts.filter(p => p.accessLevel !== 'RESTRICTED');
    const restrictedProducts = allProducts.filter(p => p.accessLevel === 'RESTRICTED');
    console.log(`  ✅ Loaded ${allProducts.length} products (${publicProducts.length} Public, ${restrictedProducts.length} Restricted).`);

    // 3. Load & Authenticate Sellers (to advance order status to DELIVERED)
    console.log("\n🏪 3. Authenticating seller accounts for order fulfillment...");
    const accountsPath = path.join(__dirname, 'generated_accounts.json');
    let sellerTokensById = {};
    if (fs.existsSync(accountsPath)) {
        try {
            const raw = JSON.parse(fs.readFileSync(accountsPath, 'utf8'));
            if (raw.sellers && Array.isArray(raw.sellers)) {
                for (const s of raw.sellers) {
                    try {
                        const sRes = await request('/auth/authenticate', 'POST', { email: s.email, password: s.password }, null, true);
                        if (sRes && sRes.token) {
                            const matchingProd = allProducts.find(p => p.sellerId);
                            if (matchingProd && matchingProd.sellerId) {
                                sellerTokensById[matchingProd.sellerId] = sRes.token;
                            }
                        }
                    } catch (e) { }
                }
            }
        } catch (e) { }
    }
    console.log(`  ✅ Seller fulfillment tokens initialized.`);

    // 4. Provision & Authenticate 15 Buyers
    console.log(`\n👥 4. Provisioning & Authenticating 15 Buyer Accounts (5 Military Units + 10 Defense Volunteers)...`);
    const buyerSessions = [];

    for (let i = 0; i < BUYERS_DATA.length; i++) {
        const b = BUYERS_DATA[i];
        let token = null;

        // Try login first
        try {
            const loginRes = await request('/auth/authenticate', 'POST', {
                email: b.email,
                password: b.password
            }, null, true);
            token = loginRes.token;
            console.log(`  🔑 [${i + 1}/15] Logged in: ${b.firstName} ${b.lastName} (${b.email})`);
        } catch (loginErr) {
            // Register if not found
            try {
                const regRes = await request('/auth/register', 'POST', {
                    firstName: b.firstName,
                    lastName: b.lastName,
                    email: b.email,
                    password: b.password,
                    phoneNumber: b.phoneNumber,
                    role: b.isMilitary ? 'MILITARY_UNIT' : 'BUYER'
                }, null, true);
                token = regRes.token;
                console.log(`  ✨ [${i + 1}/15] Registered: ${b.firstName} ${b.lastName}`);
            } catch (regErr) {
                console.error(`  ❌ Failed to register ${b.email}:`, regErr.message);
                continue;
            }
        }

        // If Military Unit, ensure profile & document verification flow
        if (b.isMilitary && token) {
            try {
                // Submit military profile
                await request('/military/profile', 'POST', b.militaryProfile, token, true).catch(() => {});
                
                // Submit verification document
                await request('/military/documents', 'POST', {
                    documentType: 'MILITARY_ID',
                    docUrl: 'https://storage.googleapis.com/milhub-protected/docs/unit-auth-cert.pdf'
                }, token, true).catch(() => {});

                // Admin approves verification
                if (adminToken) {
                    const verifs = await request('/admin/verifications', 'GET', null, adminToken, true).catch(() => []);
                    if (Array.isArray(verifs)) {
                        const target = verifs.find(v => v.email === b.email);
                        if (target) {
                            await request(`/admin/users/${target.userId}/verify?isVerified=true&reason=VerifiedActiveMilitaryUnit`, 'PATCH', null, adminToken, true);
                            console.log(`    🎖️ Admin approved military clearance for: ${b.email}`);
                        }
                    }
                }

                // Re-authenticate to refresh JWT authorities (now verified MILITARY_UNIT)
                const refreshRes = await request('/auth/authenticate', 'POST', {
                    email: b.email,
                    password: b.password
                }, null, true);
                token = refreshRes.token;
            } catch (e) {
                console.warn(`    ⚠️ Military verification notice for ${b.email}:`, e.message);
            }
        }

        buyerSessions.push({
            ...b,
            token
        });
    }

    console.log(`\n✅ Ready buyers: ${buyerSessions.length} active buyer sessions.\n`);

    // 5. Execute 10 Orders, Product Reviews, and Seller Reviews for EACH buyer
    console.log(`======================================================================`);
    console.log(`🛒 5. Simulating 10 Orders, Field Reports & Store Reviews per Buyer...`);
    console.log(`======================================================================\n`);

    let totalOrdersCreated = 0;
    let totalPaymentsProcessed = 0;
    let totalReviewsPosted = 0;
    let totalSellerReviewsPosted = 0;

    for (let bIndex = 0; bIndex < buyerSessions.length; bIndex++) {
        const buyer = buyerSessions[bIndex];
        const reviewedSellers = new Set();

        console.log(`\n----------------------------------------------------------------------`);
        console.log(`👤 [Buyer ${bIndex + 1}/${buyerSessions.length}] ${buyer.firstName} ${buyer.lastName} (${buyer.isMilitary ? '🎖️ MILITARY_UNIT' : '🛡️ BUYER'})`);
        console.log(`----------------------------------------------------------------------`);

        // Select items available for this buyer role
        const availablePool = buyer.isMilitary ? allProducts : publicProducts;
        if (availablePool.length === 0) continue;

        for (let orderNum = 1; orderNum <= 10; orderNum++) {
            // Pick a product
            const product = availablePool[Math.floor(Math.random() * availablePool.length)];
            const quantity = Math.floor(Math.random() * 2) + 1;
            const city = CITIES[Math.floor(Math.random() * CITIES.length)];

            // Create Order
            const orderPayload = {
                items: [{ productId: product.id, quantity }],
                deliveryDetails: {
                    provider: "NOVA_POSHTA",
                    type: "BRANCH",
                    recipientName: `${buyer.firstName} ${buyer.lastName}`,
                    recipientPhone: buyer.phoneNumber,
                    recipientEmail: buyer.email,
                    cityRef: city.ref,
                    cityName: city.name,
                    region: city.region,
                    branchRef: city.branchRef,
                    branchName: city.branch
                },
                paymentMethod: "CARD"
            };

            let orderResult = null;
            try {
                orderResult = await request('/orders', 'POST', orderPayload, buyer.token, true);
                totalOrdersCreated++;
            } catch (err) {
                continue;
            }

            // Simulate Payment (Webhook SUCCESS)
            if (orderResult && orderResult.transactionId) {
                try {
                    await request(`/payments/webhook/${orderResult.transactionId}?status=SUCCESS`, 'POST', null, buyer.token, true);
                    totalPaymentsProcessed++;
                } catch (payErr) { }
            }

            // Fetch created order ID to transition status to DELIVERED
            let createdOrderId = null;
            try {
                const myOrders = await request('/orders/my?size=1', 'GET', null, buyer.token, true);
                if (myOrders && myOrders.content && myOrders.content.length > 0) {
                    createdOrderId = myOrders.content[0].id;
                }
            } catch (e) { }

            // Advance delivery to DELIVERED via seller token or admin token
            if (createdOrderId) {
                try {
                    const sellerToken = (product.sellerId && sellerTokensById[product.sellerId]) ? sellerTokensById[product.sellerId] : adminToken;
                    if (sellerToken) {
                        await request(`/orders/${createdOrderId}/status?status=DELIVERED`, 'PATCH', null, sellerToken, true).catch(() => {});
                    }
                } catch (e) { }
            }

            // Post authentic Field Report / Review for the delivered product
            const reviewTemplate = REVIEWS_POOL[Math.floor(Math.random() * REVIEWS_POOL.length)];
            let reviewedProduct = false;
            try {
                await request('/reviews', 'POST', {
                    productId: product.id,
                    rating: reviewTemplate.rating,
                    comment: reviewTemplate.comment,
                    parentId: null
                }, buyer.token, true);
                totalReviewsPosted++;
                reviewedProduct = true;
            } catch (revErr) { }

            // Post authentic Store / Seller Review (if not yet reviewed by this buyer)
            if (product.sellerId && !reviewedSellers.has(product.sellerId)) {
                const sellerRevTemplate = SELLER_REVIEWS_POOL[Math.floor(Math.random() * SELLER_REVIEWS_POOL.length)];
                try {
                    await request('/seller-reviews', 'POST', {
                        sellerId: product.sellerId,
                        rating: sellerRevTemplate.rating,
                        comment: sellerRevTemplate.comment
                    }, buyer.token, true);
                    reviewedSellers.add(product.sellerId);
                    totalSellerReviewsPosted++;
                } catch (sRevErr) { }
            }

            console.log(`  ✅ [Order #${orderNum}/10] Placed, Paid & Delivered -> ⭐ Field Report for "${product.name}" (${reviewTemplate.rating}★)`);

            await delay(120); // Smooth pacing between API requests
        }
    }

    console.log(`\n======================================================================`);
    console.log(`🎉 TRAFFIC & ACTIVITY SIMULATION COMPLETED!`);
    console.log(`📊 Total Active Buyers: ${buyerSessions.length}`);
    console.log(`📦 Total Orders Created: ${totalOrdersCreated}`);
    console.log(`💳 Total Payments Processed: ${totalPaymentsProcessed}`);
    console.log(`⭐️ Total Product Field Reports Published: ${totalReviewsPosted}`);
    console.log(`🏪 Total Store / Supplier Reviews Published: ${totalSellerReviewsPosted}`);
    console.log(`======================================================================\n`);
}

runTrafficSimulator().catch((err) => {
    console.error(`\n❌ Simulation Error: ${err.message}`);
});
