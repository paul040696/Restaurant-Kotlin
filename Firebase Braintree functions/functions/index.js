const functions = require('firebase-functions');
const admin = require('firebase-admin');
admin.initializeApp();
const braintree = require('braintree');
const express = require('express');
const cors = require('cors');
const cookieParser = require('cookie-parser');



const validateFirebaseIdToken = async(req,res,next) => {
	console.log('Checking')
	if((!req.headers.authorization || !req.headers.authorization.startsWith('Bearer ')) &&
		!(req.cookies && req.cookies.__session)) {
		  res.status(403).send('Unauthorized')
		  return;
	}

	let idToken;
	if(req.headers.authorization && req.headers.authorization.startsWith('Bearer ')){
		idToken = req.headers.authorization.split('Bearer ')[1];
		}
		else if(req.cookies){
			idToken = req.cookies.__session;
		}
		else {
			req.status(403).send('Unauthorized');
			return;
		}
		try {
			const decodedIdToken = await admin.auth().verifyIdToken(idToken);
			req.user = decodedIdToken;
			next();
			return;
		}catch(error)
		{
			res.status(403).send('Unauthorized');
			return;
		}
}


//Init the app
const app = express();
app.use(cors({ origin: true}));
//app.use(cookieParser);
app.use(validateFirebaseIdToken);

var gateway = braintree.connect({

	environment: braintree.Environment.Sandbox,
				 merchantId:"qzy7dkyf94xj994j", //Fill information
				 publicKey:"cstkgvhgbtx85jgp",
				 privateKey:"4a36d902645c6124b89ec5ab17a1bf03"
});

app.get('/token',(req,response)=>{
	gateway.clientToken.generate({},(err,res) => {
		if (res) 
			response.send(JSON.stringify({error:false,token:res.clientToken}));
		else {
			response.send(JSON.stringify({error:true,errorObj:err,response:res}));
		}
	})
})

app.post('/checkout',(req,response) => {
	var transactionErrors;
	var amount = req.body.amount;
	var nonce = req.body.payment_method_nonce;

	gateway.transaction.sale({
		amount:amount,
		payment_method_nonce:nonce,
		options:{
			submitForSettlement:true

		}
	},(error,result) => {
		if(response.success || result.transaction)
		{
			response.send(JSON.stringify(result))
		}
		else {
			transactionErrors = result.errors.deepErrors();
			response.send(JSON.stringify(formatErrors(transactionErrors)))
		}
	});
});

exports.widgeets = functions.https.onRequest(app);