const fs = require('fs');
const path = require('path');

// Usage:
//   node seed.js         -> Default: Local (http://localhost:8080/api/v1)
//   node seed.js cloud   -> Cloud (https://milhub-api-gateway-258044247462.us-central1.run.app/api/v1)
//   node seed.js <url>   -> Custom API Gateway URL
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

console.log(`=======================================================`);
console.log(`🚀 Starting MilHub Database Seeder in [${displayMode}] mode`);
console.log(`📍 Target API Base URL: ${API_BASE}`);
console.log(`=======================================================\n`);

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
                console.error(`  ⚠️ [${method} ${endpoint}] HTTP ${res.status} (${duration}ms)`);
            }
            throw new Error(`API Error [${method} ${endpoint}]: ${res.status} ${text}`);
        }

        try { return JSON.parse(text); } catch { return text; }
    } catch (err) {
        throw err;
    }
}

async function delay(ms) { return new Promise(resolve => setTimeout(resolve, ms)); }

async function waitForApi() {
    console.log('--- ⏳ Connecting to API Gateway ---');
    let attempts = 0;
    while (true) {
        attempts++;
        try {
            const start = Date.now();
            const res = await fetch(`${API_BASE}/categories/`);
            const duration = Date.now() - start;
            if (res.ok) {
                console.log(`✅ API Gateway is online & responsive! (${duration}ms, HTTP ${res.status})\n`);
                break;
            } else {
                console.log(`  Attempt #${attempts}: Waiting for Gateway... (HTTP ${res.status})`);
            }
        } catch (e) {
            console.log(`  Attempt #${attempts}: Waiting for Gateway... (${e.message})`);
        }
        await delay(3000);
    }
}

async function runMegaSeeder() {
    await waitForApi();

    const megaCatalogPath = path.join(__dirname, 'seed-mega-catalog.json');
    const seedDataPath = path.join(__dirname, 'seed-data.json');

    if (!fs.existsSync(megaCatalogPath) || !fs.existsSync(seedDataPath)) {
        console.error('❌ Missing seed JSON files (seed-mega-catalog.json or seed-data.json)');
        process.exit(1);
    }

    const megaCatalog = JSON.parse(fs.readFileSync(megaCatalogPath, 'utf-8')).catalog;
    const seedData = JSON.parse(fs.readFileSync(seedDataPath, 'utf-8'));
    const accounts = [];

    // 1. Register & Authenticate Sellers First
    console.log('🏪 1. Registering & Authenticating Seller Accounts...');
    const sellerTokens = [];
    for (const s of seedData.sellers) {
        console.log(`\n  ➡️ Processing seller: [${s.name}] (${s.email})`);
        const phone = `+380${Math.floor(100000000 + Math.random() * 900000000)}`;

        // Register seller account
        try {
            await request('/auth/register', 'POST', {
                firstName: s.name.split(' ')[0],
                lastName: 'Supplier',
                email: s.email,
                password: 'Password123!',
                role: 'SELLER',
                phoneNumber: phone
            }, null, true);
            console.log(`    ✅ Registered account: ${s.email}`);
        } catch (e) {
            console.log(`    ℹ️ Account ${s.email} exists.`);
        }

        try {
            // Authenticate initial token (BUYER role)
            const sLogin = await request('/auth/authenticate', 'POST', { email: s.email, password: 'Password123!' }, null, true);
            console.log(`    🔑 Initial token obtained: ${s.email}`);

            // Seller Profile
            try {
                await request('/sellers/profile', 'POST', { companyName: s.name, taxId: s.taxId, logoUrl: s.logoUrl }, sLogin.token, true);
                console.log(`    🏢 Created profile: ${s.name}`);
            } catch (e) {
                console.log(`    ℹ️ Profile ${s.name} ready.`);
            }

            // Pickup point
            try {
                await request('/sellers/points', 'POST', {
                    name: `Main Office ${s.name}`,
                    cityRef: "00000000-0000-0000-0000-000000000000",
                    cityName: s.city,
                    streetName: "Central St",
                    building: "1"
                }, sLogin.token, true);
                console.log(`    📍 Created pickup point for: ${s.name}`);
            } catch (e) {
                console.log(`    ℹ️ Pickup point ready.`);
            }

            // Verification document submission (using REGISTRATION_CERT enum)
            try {
                await request('/users/me/verification-docs', 'POST', {
                    documentType: 'REGISTRATION_CERT',
                    docUrl: `https://storage.googleapis.com/docs/${s.taxId}.pdf`
                }, sLogin.token, true);
                console.log(`    📄 Submitted verification document for: ${s.name}`);
            } catch (e) {
                console.log(`    ℹ️ Verification document ready.`);
            }

            sellerTokens.push({ token: sLogin.token, name: s.name, email: s.email });
            accounts.push({ company: s.name, email: s.email, password: 'Password123!', taxId: s.taxId, city: s.city, phone: phone });
        } catch (e) {
            console.error(`\n======================================================================`);
            console.error(`❌ [SELLER AUTH FAILED] Could not authenticate seller: ${s.name} (${s.email})`);
            console.error(`👉 Details: ${e.message}`);
            console.error(`======================================================================\n`);
        }
    }
    console.log(`\n✅ Initial seller accounts prepared: ${sellerTokens.length}\n`);

    if (sellerTokens.length === 0) {
        console.error('❌ No seller accounts could be created or authenticated.');
        process.exit(1);
    }

    let masterAuthToken = sellerTokens[0].token;

    // 2. Admin Authentication
    console.log('🔑 2. Authenticating System Administrator...');
    const adminCandidates = [
        { email: 'admin@milhub.ua', password: 'Password123!' },
        { email: 'admin@milhub.com', password: 'Password123!' }
    ];
    let adminToken = null;
    let adminErrorDetail = '';

    for (const cand of adminCandidates) {
        try {
            const res = await request('/auth/authenticate', 'POST', cand, null, true);
            if (res && res.token) {
                adminToken = res.token;
                masterAuthToken = res.token;
                console.log(`  ✅ Admin authenticated successfully as [${cand.email}]\n`);
                break;
            }
        } catch (e) {
            adminErrorDetail = e.message;
        }
    }

    if (!adminToken) {
        console.error(`\n======================================================================`);
        console.error(`🚨 [CRITICAL WARNING] ADMIN AUTHENTICATION FAILED!`);
        console.error(`❌ Could not log in as admin@milhub.ua or admin@milhub.com.`);
        console.error(`👉 Server Response: ${adminErrorDetail || 'HTTP 401 Unauthorized'}`);
        console.error(`⚠️ CONSEQUENCE: Seller accounts CANNOT be verified without Admin approval.`);
        console.error(`⚠️ Product creation will fail (HTTP 403) for unverified sellers!`);
        console.error(`======================================================================\n`);
    }

    // 3. Admin Verification of Sellers
    if (adminToken) {
        console.log('🛡️ 3. Admin Verifying Pending Seller Requests...');
        try {
            const verifs = await request('/admin/verifications', 'GET', null, adminToken, true);
            if (Array.isArray(verifs)) {
                for (const v of verifs) {
                    try {
                        await request(`/admin/users/${v.userId}/verify?isVerified=true&reason=AutoSeederApproved`, 'PATCH', null, adminToken, true);
                        console.log(`  ✅ Approved seller: ${v.email}`);
                    } catch (e) { }
                }
            }
        } catch (e) { }
        console.log(`✅ Admin seller verifications complete.\n`);

        // --- RE-AUTHENTICATE SELLERS TO REFRESH JWT TOKENS WITH SELLER AUTHORITIES ---
        console.log('🔑 Re-authenticating verified sellers to issue updated SELLER JWT tokens...');
        for (let i = 0; i < sellerTokens.length; i++) {
            const s = sellerTokens[i];
            try {
                const sLogin = await request('/auth/authenticate', 'POST', { email: s.email, password: 'Password123!' }, null, true);
                sellerTokens[i].token = sLogin.token;
                console.log(`  🔑 Refreshed seller JWT token: ${s.email}`);
            } catch (e) {
                console.error(`  ⚠️ Failed to refresh token for ${s.email}: ${e.message}`);
            }
        }
        console.log('✅ Seller tokens updated with SELLER permissions.\n');
    }

    // 4. Categories Hierarchy Check & Seeding
    const categoryIdMap = {};
    const masterMap = {};

    console.log('📂 4. Seeding & Validating Categories Hierarchy...');

    // Quick existence check: Fetch existing categories first
    const existingCategories = await request('/categories/', 'GET', null, null, true).catch(() => []);
    if (Array.isArray(existingCategories) && existingCategories.length >= 3) {
        console.log(`  ℹ️ Found ${existingCategories.length} existing categories in API. Mapping category IDs...`);
        for (const cat of existingCategories) {
            masterMap[cat.name] = cat.id;
            categoryIdMap[cat.name] = cat.id;
            if (cat.subCategories && Array.isArray(cat.subCategories)) {
                for (const sub of cat.subCategories) {
                    categoryIdMap[`${cat.name}: ${sub.name}`] = sub.id;
                }
            }
        }
    }

    async function ensureCategory(catString) {
        if (categoryIdMap[catString]) return categoryIdMap[catString];

        const parts = catString.split(':');
        const masterName = parts[0].trim();
        const subName = parts[1] ? catString.trim() : null;

        // Ensure Master Category
        if (!masterMap[masterName]) {
            try {
                const m = await request('/categories/', 'POST', { name: masterName, description: `Master category ${masterName}` }, masterAuthToken, true);
                masterMap[masterName] = m.id;
                console.log(`  📂 Created master category: "${masterName}"`);
            } catch (e) {
                const all = await request('/categories/', 'GET', null, null, true).catch(() => []);
                const ex = Array.isArray(all) ? all.find(c => c.name === masterName && !c.parentId) : null;
                if (ex) {
                    masterMap[masterName] = ex.id;
                }
            }
        }

        // Ensure Sub Category or fallback to Master Category ID
        if (!subName) {
            categoryIdMap[catString] = masterMap[masterName];
        } else {
            try {
                const s = await request('/categories/', 'POST', { name: subName, parentId: masterMap[masterName] }, masterAuthToken, true);
                categoryIdMap[catString] = s.id;
                console.log(`    📁 Created sub category: "${subName}"`);
            } catch (e) {
                const all = await request('/categories/', 'GET', null, null, true).catch(() => []);
                const masterCat = Array.isArray(all) ? all.find(c => c.id === masterMap[masterName]) : null;
                const ex = masterCat && masterCat.subCategories ? masterCat.subCategories.find(c => c.name === subName) : null;
                if (ex) {
                    categoryIdMap[catString] = ex.id;
                }
            }
        }
        return categoryIdMap[catString];
    }

    for (const item of megaCatalog) {
        await ensureCategory(item.category);
    }
    console.log('✅ Categories hierarchy ready.\n');

    await delay(500);

    // 5. Delivery Locations Check & Import
    if (seedData.locations && seedData.locations.length > 0) {
        console.log(`🚚 5. Validating & Importing Delivery Locations (${seedData.locations.length} cities)...`);
        const existingLocations = await request('/delivery/locations', 'GET', null, null, true).catch(() => []);
        if (Array.isArray(existingLocations) && existingLocations.length >= 10) {
            console.log(`  ℹ️ Delivery locations already populated (${existingLocations.length} locations). Skipping import.\n`);
        } else {
            try {
                await request('/delivery/locations/import', 'POST', seedData.locations, masterAuthToken, true);
                console.log('✅ Delivery locations imported successfully.\n');
            } catch (e) { }
        }
    }

    // 6. Products Catalog Seeding
    console.log(`📦 6. Seeding Product Catalog (${megaCatalog.length} products)...`);
    let count = 0;
    for (const item of megaCatalog) {
        const catId = await ensureCategory(item.category);

        if (!catId) {
            continue;
        }

        const seller = sellerTokens[count % sellerTokens.length];
        if (!seller) continue;

        try {
            await request('/products', 'POST', {
                name: item.name,
                description: `Specialized tactical equipment: ${item.name}. Official warranty and certified quality from manufacturer ${seller.name}.`,
                price: item.price,
                quantity: 100,
                categoryId: catId,
                accessLevel: item.access || 'PUBLIC',
                weight: 1.0, length: 1, width: 1, height: 1,
                previewImageUrl: item.images ? item.images[0] : '',
                imageUrls: item.images || []
            }, seller.token, true);
            count++;
            if (count % 20 === 0 || count === megaCatalog.length) {
                console.log(`  📦 Progress: ${count}/${megaCatalog.length} products...`);
            }
        } catch (e) {
            if (e.message.includes('409')) {
                count++;
            } else {
                console.error(`  ❌ [PRODUCT CREATE FAILED] "${item.name}": ${e.message}`);
            }
        }
    }

    const outputPath = path.join(__dirname, 'generated_accounts.json');
    fs.writeFileSync(outputPath, JSON.stringify({
        sellers: accounts
    }, null, 2));

    console.log(`\n=======================================================`);
    console.log(`🎉 SEEDING PROCESS COMPLETED!`);
    console.log(`✅ Total products processed: ${count}/${megaCatalog.length}`);
    console.log(`📄 Generated seller accounts saved to: generated_accounts.json`);
    console.log(`=======================================================\n`);
}

runMegaSeeder().catch((err) => {
    console.error(`\nℹ️ Notice: ${err.message}`);
});
