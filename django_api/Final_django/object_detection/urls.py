from django.urls import path
from .views import predict, ping

urlpatterns = [
    path('predict/', predict, name='predict'),
    path('ping/', ping, name='ping'),
]
