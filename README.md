# Restaurant-Kotlin

An Android Kotlin app for home delivery solutions.

## Introduction

This project represents a software app designed to provide a necessary platform for the restaurants and catering services that want to impllement a home delivery service.

The project is divided in 2 sections: client app and server app.

## Client app

The main functionalities of the app are:

- login/create account with Firebase Auth using phone number.
- select product and:
    - view product description and rating
    - customize product( number of items, extra ingredients, select size)
    - add rating and review
    - add to cart
- select cart and:
    - modify shopping list
    - clear cart
    - place order
 - place order:
    - to current location using geolocation/ custom defined location/ account's defined location
    - select payment method: Cash/ Card with Baintree payment
 - view orders:
    - view details about previous placed orders
    - repeat order 

Demo:

The cart
<img src="https://user-images.githubusercontent.com/29239337/94031277-5c796400-fdc7-11ea-8c3f-78a46547948f.png" width="150">
Shipping details
<img src="https://user-images.githubusercontent.com/29239337/94031279-5d11fa80-fdc7-11ea-86c3-f0ade296571f.png" width="150">
Product description
<img src="https://user-images.githubusercontent.com/29239337/94031283-5d11fa80-fdc7-11ea-96f4-627e5e9a2249.png" width="150">
Previous orders
<img src="https://user-images.githubusercontent.com/29239337/94031287-5daa9100-fdc7-11ea-829f-0b6bd1a25b9d.png" width="150">


## Server app

The main functionalities of the app are:

- login/create account with Firebase Auth using phone number
- add or remove category/product
- modify category description: name and photo
- modify product description(name, description, photo) and options(Extra ingredients, available sizes)
- view and filter orders
- search product functionality
- modify order status

Demo: 



